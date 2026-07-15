# Authentication — access + refresh tokens (RS256)

This document describes the token model the backend now uses and, at the end, the
exact contract an **API gateway** needs to sit in front of it. It replaces the old
single-JWT-plus-blacklist scheme.

## The model

Two credentials, with deliberately different lifetimes and jobs:

| | Access token | Refresh token |
|---|---|---|
| Form | RS256 JWT (signed claims) | opaque 256-bit random string |
| Lifetime | short (15 min default) | long (7 days default) |
| Carried in | `Authorization: Bearer …` (from an in-memory store) | httpOnly cookie `refresh_token`, `Path=/api/auth/v1` |
| Verified by | signature only — **no DB lookup** | lookup in Postgres (`refresh_tokens`, keyed by `sha256(token)`) |
| Purpose | prove identity on every request | mint new access tokens; the only revocable credential |

Why this shape:

- **The access token is stateless.** Verifying it is pure signature math against the
  RSA public key — no revocation-list lookup on the hot path. That's what lets
  verification move to a gateway (or scale across replicas) with zero shared state.
- **Revocation lives with the refresh token.** Logout deletes the refresh token row,
  so no new access tokens can be minted. Any already-issued access token
  keeps working until it expires (≤15 min) — the standard, accepted trade-off of
  dropping the per-request blacklist.
- **The refresh token is opaque and httpOnly.** It carries no readable identity and
  JavaScript can't touch it, so an XSS bug can't exfiltrate the long-lived
  credential. It's also hashed at rest, so a database dump yields nothing usable.
- **Rotation.** Every `/refresh` atomically consumes the presented token (a
  delete-by-hash that must affect exactly one row) and issues a replacement. A token
  replayed after use fails — the basic
  theft-detection guarantee.

## Endpoints (all under `/api/auth/v1`, all public)

| Endpoint | Does |
|---|---|
| `POST /login` | validate credentials → access token in body + `Set-Cookie: refresh_token` |
| `POST /refresh` | read refresh cookie → rotate it → new access token in body + rotated cookie |
| `POST /logout` | delete refresh token row + clear the cookie |
| `POST /oauth-token` | redeem the OAuth exchange code → same handoff as `/login` |
| `POST /sign-up` | create a pending verification (no tokens yet) |

Email verification (`POST /api/verification/v1/verify`) also completes a login, so it
returns an access token + sets the refresh cookie, exactly like `/login`.

Response body shape for the login-like endpoints:

```json
{ "accessToken": "<jwt>", "id": 1, "email": "…", "name": "…", "role": "ROLE_USER" }
```

## Access-token claims

```
sub  = email
id, email, name, role           (role = ROLE_USER | ROLE_ADMIN | ROLE_ORGANIZATION)
profileComplete                 (users only)
iat, exp
```

## Keys

RS256. The backend signs with the **private** key; everything else verifies with the
**public** key. Generate a keypair (base64 DER, single line — friendly for env vars):

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out priv.pem
openssl rsa -pubout -in priv.pem -out pub.pem
JWT_PRIVATE_KEY=$(grep -v '^-' priv.pem | tr -d '\n')   # backend only
JWT_PUBLIC_KEY=$(grep -v '^-' pub.pem  | tr -d '\n')    # backend + gateway
```

Config knobs (`application.properties`, all env-overridable):
`jwt.private-key`, `jwt.public-key`, `jwt.access-token-expiration-ms`,
`jwt.refresh-token-expiration-ms`, `app.cookie.secure`, `app.cookie.same-site`.

## Frontend flow

- Access token lives only in memory (`src/auth/tokenStore.js`), never in
  `sessionStorage`/`localStorage`, so it isn't readable via storage APIs from an
  injected script. The axios client attaches it as a Bearer header and runs with
  `withCredentials: true` so the refresh cookie is sent.
- On any `401`, a response interceptor makes **one** `/refresh` attempt (single-flight,
  so concurrent 401s share it), stores the new access token, and replays the request.
  If refresh fails, the user is signed out.
- On app load, if there's no valid access token, `AuthContext` tries a silent
  `/refresh` — so a returning visitor with a live refresh cookie is logged straight
  back in without re-entering credentials.

---

## The gateway (Spring Cloud Gateway, `gateway/`)

The gateway is now the **single entry point** — the only service with a published
host port. The browser only ever talks to it; the backend, frontend, and everything
else are internal to the Docker network. It:

1. **Serves the SPA and proxies everything** from one origin: `/` → frontend nginx,
   `/api/**` → backend, `/ws/**` → watch-party, plus the OAuth handshake
   (`/oauth2/authorize/**`, `/login/oauth2/**`) → backend. One origin means no CORS,
   and the refresh cookie is `SameSite=Lax`.

2. **Verifies access tokens with the public key only** (`JWT_PUBLIC_KEY`; never the
   private key) — pure RS256 signature + expiry, no call back to the backend, no Redis.
   Verification is *opportunistic* (a missing/invalid token isn't rejected by the
   verify step itself; the route matrix decides), mirroring the backend's old filter
   so public endpoints keep working when the browser sends a stale token.

3. **Enforces the public/protected route matrix** at the edge — the same rules the
   backend had, driven by the token's `role` claim (`/api/admin/**` → `ROLE_ADMIN`,
   etc.). Public: auth, verification, sign-up, health, movie browse, `/ws`.

4. **Forwards a trusted identity** to the backend as `X-User-Id/Role/Email/Name`,
   after **stripping any inbound copies** so a client can't forge them. The backend
   now trusts these headers (see `GatewayAuthenticationFilter`) instead of parsing
   JWTs — safe because the backend has no host port and is reachable only through the
   gateway. Fine-grained checks ("is this user the author of post 42?") still live in
   the backend.

5. **Rate-limits** (Bucket4j, Redis-backed): a tight per-IP bucket on `/api/auth/**`
   and a per-user (else per-IP) bucket on the rest, returning `429` +
   `X-RateLimit-Remaining` + `Retry-After`.

Issuance (signing tokens, the refresh-token store, the user database) stays entirely
in the backend — the gateway is a verifier and traffic cop, never a minter. To scale
the gateway horizontally, nothing changes: token verification is stateless, and its
only shared state — the rate-limit buckets — lives in Redis. (The refresh-token store
is the backend's concern, in Postgres, not the gateway's.)
