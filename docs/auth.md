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
| Carried in | `Authorization: Bearer …` (from `sessionStorage`) | httpOnly cookie `refresh_token`, `Path=/api/auth/v1` |
| Verified by | signature only — **no DB, no Redis** | lookup in Redis (`refresh-token:<sha256>`) |
| Purpose | prove identity on every request | mint new access tokens; the only revocable credential |

Why this shape:

- **The access token is stateless.** Verifying it is pure signature math against the
  RSA public key — no revocation-list lookup on the hot path. That's what lets
  verification move to a gateway (or scale across replicas) with zero shared state.
- **Revocation lives with the refresh token.** Logout deletes the refresh token from
  Redis, so no new access tokens can be minted. Any already-issued access token
  keeps working until it expires (≤15 min) — the standard, accepted trade-off of
  dropping the per-request blacklist.
- **The refresh token is opaque and httpOnly.** It carries no readable identity and
  JavaScript can't touch it, so an XSS bug can't exfiltrate the long-lived
  credential. It's also hashed at rest, so a Redis dump yields nothing usable.
- **Rotation.** Every `/refresh` atomically consumes the presented token (Redis
  `GETDEL`) and issues a replacement. A token replayed after use fails — the basic
  theft-detection guarantee.

## Endpoints (all under `/api/auth/v1`, all public)

| Endpoint | Does |
|---|---|
| `POST /login` | validate credentials → access token in body + `Set-Cookie: refresh_token` |
| `POST /refresh` | read refresh cookie → rotate it → new access token in body + rotated cookie |
| `POST /logout` | delete refresh token from Redis + clear the cookie |
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

- Access token lives in `sessionStorage`; the axios client attaches it as a Bearer
  header and runs with `withCredentials: true` so the refresh cookie is sent.
- On any `401`, a response interceptor makes **one** `/refresh` attempt (single-flight,
  so concurrent 401s share it), stores the new access token, and replays the request.
  If refresh fails, the user is signed out.
- On app load, if there's no valid access token, `AuthContext` tries a silent
  `/refresh` — so a returning visitor with a live refresh cookie is logged straight
  back in without re-entering credentials.

---

## What the gateway needs (the handoff)

When you build the gateway, it should own **verification and rate limiting** and stay
out of **issuance**. Concretely:

1. **Verify access tokens with the public key.** Give the gateway `JWT_PUBLIC_KEY`
   only — never the private key. It validates the RS256 signature and expiry locally;
   no call back to the backend, no Redis. Reject anything that fails.

2. **Leave these routes public** (no access token expected — they're how you *get* one):
   - `POST /api/auth/**` (login, refresh, logout, oauth-token, sign-up)
   - `POST /api/verification/**`
   - `GET  /api/health/**`
   - `GET  /api/movie/**` (currently browseable unauthenticated)
   - the OAuth2 browser redirects: `/oauth2/**`, `/login/oauth2/**`

3. **Forward cookies untouched.** `/refresh` and `/logout` depend on the
   `refresh_token` cookie reaching the backend, so the gateway must pass `Cookie`
   through and pass `Set-Cookie` back. Once the gateway makes the frontend and API
   share one origin, switch the cookie to `SameSite=Lax` (set `AUTH_COOKIE_SAME_SITE=Lax`)
   and you can drop the cross-origin CORS config entirely.

4. **Coarse authorization from the `role` claim** is fine at the edge (e.g.
   `/api/admin/**` requires `ROLE_ADMIN`). **Fine-grained** checks ("is this user the
   author of post 42?") stay in the backend — they need domain data the gateway
   shouldn't have. The backend keeps its own `JWTAuthenticationFilter` as
   defense-in-depth; it performs the identical public-key check.

5. **Inject identity as trusted headers, and strip inbound ones.** After verifying,
   the gateway can add e.g. `X-User-Id`, `X-User-Role` for downstream services — but
   it MUST first delete any such headers arriving from the client, or a caller could
   forge them. (The backend today reads identity from the JWT it re-verifies, so this
   is only needed if/when you make the backend trust gateway headers instead.)

6. **Rate limiting** belongs here too, keyed by user-id when a valid token is present
   and by IP otherwise, backed by Redis. A much tighter per-IP bucket on
   `/api/auth/**` blunts credential-stuffing.

Issuance (signing tokens, the refresh-token store, the user database) stays entirely
in the backend. The gateway is a verifier and traffic cop, never a minter.
