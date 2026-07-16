# API Gateway — Architecture & Feature Overview

This document describes the Cinemate **API gateway**: the single entry point that sits in
front of every service, what it does, what it deliberately does *not* do, and everything that
changed elsewhere because of it.

- Auth/token internals (RS256, refresh tokens, cookie): [`auth.md`](auth.md)
- Environment variables and running the stack: [`deployment.md`](deployment.md)

---

## 1. TL;DR

- The gateway (`gateway/`, Spring Cloud Gateway, servlet flavour) is the **only service
  with a published host port**. The browser talks to it and nothing else.
- It **serves the SPA, proxies every API and WebSocket call, verifies access tokens,
  enforces authorization, injects a trusted identity to the backend, and rate-limits.**
- Everything is now **one origin**, so there is no CORS, the refresh cookie is
  `SameSite=Lax`, and the CSP is tight.
- The backend, frontend, watch-party, the content-moderation services, and all Redis
  instances are **internal to the Docker network** — unreachable from the host.
- **Yes, the WebSocket passes through the gateway** (`/ws` → watch-party). See §7.

---

## 2. Before vs. after

**Before** — the browser talked to two origins, and every service did its own auth:

```
browser ──▶ frontend nginx  :5173   (SPA + /ws proxy)
browser ──▶ backend         :8080   (/api, validates JWT on every request, sets CORS)
                              backend ──▶ watch-party (internal) · kafka (moderation)
```

**After** — the browser talks only to the gateway; auth is at the edge:

```
                 ┌───────────────────────── gateway :8080 (ONLY published port) ─────────────────────────┐
  browser ──────▶│  verify JWT · route/role matrix · inject X-User-* · rate-limit                          │
                 │     /                    ─▶ frontend nginx  :80     (static SPA)                        │
                 │     /api/watch-party/**  ─▶ watch-party     :8081   (REST, owns the domain)              │
                 │     /api/**              ─▶ backend         :8080   (trusts X-User-* headers, no JWT)    │
                 │     /ws/**               ─▶ watch-party     :8081   (SockJS / STOMP)                     │
                 │     /oauth2/authorize/**, /login/oauth2/** ─▶ backend (Google handshake)                 │
                 └──────────────────────────────────────────────────────────────────────────────────────┘
                        backend ──▶ postgres · kafka                             (all internal)
                        watch-party ──▶ redis · backend /api/movie (read-only)       (internal)
```

Nothing except the gateway (and the raw database port, for dev convenience) is
reachable from the host.

---

## 3. What the gateway DOES

| Responsibility | How |
|---|---|
| **Single entry point** | Only published port; serves SPA + proxies all APIs/WS. |
| **Reverse proxy / routing** | Path-based routes to backend, watch-party, frontend (§6). |
| **Token verification** | RS256 signature + expiry, using the **public key only** (`GatewayJwtAuthenticationFilter` + a `NimbusJwtDecoder`). Never holds the private key, so it can verify but never mint. |
| **Authorization (coarse)** | The public/protected route + role matrix, driven by the token's `role` claim (`SecurityConfig`, §5). |
| **Identity forwarding** | Injects `X-User-Id/Role/Email/Name` to the backend from the verified token, **stripping any inbound copies** first (§4). |
| **Rate limiting** | Redis-backed Bucket4j token buckets, per-user / per-IP hybrid (§8). |
| **CORS elimination** | By unifying to one origin, CORS is no longer needed anywhere. |

## 4. What the gateway does NOT do

| Not its job | Where it lives instead |
|---|---|
| **Issue / sign tokens** | Backend (`JWTProvider` signs with the private key). |
| **Store / rotate refresh tokens** | Backend (`RefreshTokenService`, Postgres `refresh_tokens` table). Login/refresh/logout are just proxied through. |
| **Fine-grained authorization** ("is this user the author of post 42?", "does this org own this movie?") | Backend — it needs domain data the gateway shouldn't have. |
| **Business logic / DB access** | Backend and the other services. The gateway has no database. |
| **Talk to Postgres** | Only the backend does. The gateway's only stateful dependency is Redis (for rate-limit buckets). |
| **Per-message WebSocket auth** | Not yet done at the gateway (tracked as REL-08 in [`tech-debt.md`](tech-debt.md)). The gateway proxies the `/ws` handshake but does not authenticate individual STOMP frames — watch-party verifies the JWT itself in the STOMP CONNECT frame instead. |
| **Service discovery / load balancing logic** | Static routing by Docker DNS name. No Eureka/Consul (overkill for Compose). |

## 5. Authorization: public vs. protected routes

The gateway mirrors the matrix the backend used to enforce. Verification is
**opportunistic** — a missing or invalid token is not rejected by the verify step
itself; the matrix below decides. (This is deliberate: it keeps public endpoints
working even when the browser tacks a stale token onto the request, exactly as the
old backend filter behaved.)

**Public (no token required):**

- `OPTIONS /**` (CORS preflight, harmless now that it's one origin)
- `/actuator/health` (the gateway's own health probe)
- `/api/auth/**` — login, refresh, logout, oauth-token, sign-up
- `/api/verification/**`
- `/api/user/v1/sign-up`
- `/api/health/**`
- `/api/movie/**` — movie browsing is intentionally unauthenticated
- `/ws/**` — WebSocket handshake (its own per-message auth is separate, REL-08)
- everything non-API (the SPA, assets, client-side routes like `/forum/123`)

**Protected (token + role required):**

| Path | Required authority |
|---|---|
| `/api/admin/**` | `ROLE_ADMIN` |
| `/api/organization/**` | `ROLE_ORGANIZATION` |
| `/api/user/**` | `ROLE_USER` |
| `/api/movie-review/**`, `/api/watch-history/**`, `/api/liked-movie/**`, `/api/watch-later/**`, `/api/forum/**`, `/api/comment/**`, `/api/vote/**`, `/api/forum-follow/**`, `/api/feed/**`, `/api/post/**`, `/api/watch-party/**` | `ROLE_USER` |
| any other `/api/**` | authenticated (fallback) |

Unauthenticated on a protected route → **401**. Authenticated but wrong role → **403**.
Both statuses are written **directly** (not via `sendError`), because `sendError`
dispatches to `/error`, which the `/**`→frontend catch-all would otherwise proxy —
silently turning a 401/403 into a 200 SPA page. (That bug was caught and fixed during
the cutover.)

## 6. Routing table

Defined in `gateway/src/main/resources/application.yml`. Order matters — the `/**`
catch-all is last so the specific routes win.

| Route id | Predicate (path) | Upstream |
|---|---|---|
| `backend` | `/api/**`, `/oauth2/authorize/**`, `/login/oauth2/**` | `http://backend:8080` |
| `watch-party-api` | `/api/watch-party/**` | `http://watch-party:8081` |
| `watch-party` | `/ws/**` | `http://watch-party:8081` |
| `frontend` | `/**` (catch-all) | `http://frontend:80` (nginx serves the SPA) |

Upstream hosts are env-overridable (`BACKEND_URI`, `WATCHPARTY_URI`, `FRONTEND_URI`).
Note the OAuth route is `/oauth2/authorize/**` (Spring's initiation endpoint) and
`/login/oauth2/**` (the callback) — deliberately **not** the broad `/oauth2/**`, because
the SPA has its own `/oauth2/redirect` page that must reach the frontend. The
`watch-party-api` route must be ordered **before** the generic `backend` route so
`/api/watch-party/**` doesn't fall through to `/api/**`.

## 7. WebSockets — yes, they go through the gateway

`/ws/**` is routed to the watch-party service. The frontend opens a SockJS/STOMP
connection to `/ws` on the same origin (the gateway), and the gateway proxies it to
watch-party. Previously the browser reached watch-party via the frontend nginx's `/ws`
proxy; now the gateway owns it, so it's genuinely one entry point for WebSocket traffic
too.

Because the servlet gateway's raw-WebSocket upgrade support is weaker than the reactive
gateway's, this was verified explicitly: `/ws/info` proxies correctly and a full SockJS
session opens through the gateway (the server returns the `o` open frame, and the
watch-party origin check passes). If a browser's raw-WebSocket transport is ever blocked,
SockJS automatically falls back to same-origin HTTP transports, which the gateway proxies
without issue. The tightened CSP (`connect-src 'self'`) permits this because it's all
same-origin.

What the gateway does **not** do for WebSockets: it does not authenticate individual
STOMP messages. Per-connection/per-message WS auth remains a separate, open item
(REL-08) — watch-party verifies the JWT itself in the STOMP CONNECT frame, since the
token can't arrive via the gateway's trusted-header mechanism on a WebSocket upgrade.

## 8. Rate limiting

Token-bucket limiting with **Bucket4j**, backed by **Redis** (Lettuce, the dedicated
`redis-ratelimit` instance) so counters are shared across gateway replicas.
Implemented in `RateLimitFilter` (+ `RateLimitConfig`), running right after identity is
established and before authorization.

| Scope | Key | Bucket |
|---|---|---|
| `/api/auth/**` | `rl:auth:ip:<ip>` | capacity 10, refill 5/s |
| authenticated `/api/**` | `rl:user:<id>` (from the verified `X-User-Id`) | capacity 40, refill 20/s |
| anonymous `/api/**` | `rl:ip:<ip>` | capacity 40, refill 20/s |

- Only `/api/**` is limited; the SPA and its static assets are not.
- The tight per-IP bucket on `/api/auth/**` blunts credential-stuffing and refresh
  hammering (there's no user yet at login).
- Rejections return **429** with `Retry-After` and `X-RateLimit-Remaining: 0`; allowed
  requests carry `X-RateLimit-Remaining`.
- Client IP prefers `X-Forwarded-For` (for real deployments behind a load balancer),
  falling back to the socket address. In local Compose all host traffic shares the
  Docker bridge IP, so it lumps into one IP bucket — expected in dev.
- The Redis connection is opened **lazily** on first request, so the gateway still boots
  if `redis-ratelimit` is briefly unavailable.

## 9. Trusted-header contract (gateway → backend)

After verifying the token, the gateway forwards identity to the backend as headers and
**deletes any inbound copies** so a client can't forge them:

| Header | Value |
|---|---|
| `X-User-Id` | numeric user id |
| `X-User-Role` | `ROLE_USER` / `ROLE_ADMIN` / `ROLE_ORGANIZATION` |
| `X-User-Email` | account email |
| `X-User-Name` | display name |

The backend's `GatewayAuthenticationFilter` reads these and reconstructs the exact same
`SecurityContext` and request attributes (`userId`, `userRole`, `userEmail`, `userName`)
the old JWT filter produced — so `@PreAuthorize` and every controller are unchanged, but
the backend does **no** token crypto on the request path. Watch-party has its own
analogous `GatewayAuthenticationFilter` for its REST endpoints, trusting the same headers.

**Why this is safe:** the backend has no published host port. It's reachable only across
the internal `app-net` Docker network, through the gateway, which strips inbound
`X-User-*`. The trust boundary is "the internal network." (If you later needed to defend
against a compromised internal service, you'd sign the headers or mTLS between gateway
and backend — not needed for this deployment.)

## 10. Single-origin consequences

Because the browser now only ever hits the gateway origin:

- **CORS removed** from the backend entirely (it's internal; nothing calls it
  cross-origin). `CORS_ALLOWED_ORIGINS` survives only for watch-party's WS origin check.
- **Refresh cookie** is `SameSite=Lax` and not `Secure` (works over `http://localhost`),
  scoped to `Path=/api/auth/v1`.
- **CSP** `connect-src` tightened to `'self'` (was `'self' http: https: ws: wss:`).
- **Frontend API base is relative** — `config.js` sets `API_BASE_URL: ""`, so the SPA
  calls `/api` and `/ws` on whatever origin served it. No hard-coded backend URL.

## 11. Ports & topology (Docker Compose)

| Service | Host port | Container | Notes |
|---|---|---|---|
| **gateway** | **8080** | 8080 | the only app port exposed to the host |
| backend | — | 8080 | internal only (`expose`) |
| frontend | — | 80 | internal only; nginx serves the SPA behind the gateway |
| watch-party | — | 8081 | internal only |
| kafka | — | 9092 | internal only (moderation pipeline log; no auth) |
| moderation-worker | — | — | internal only (ONNX inference; no port) |
| redis | — | 6379 | internal only (watch-party state) |
| redis-ratelimit | — | 6379 | internal only (gateway rate-limit buckets) |
| postgres | 5433 | 5432 | host port kept for dev DB access |

**You now open the app at `http://localhost:8080`** (not `:5173`).

## 12. Configuration reference (gateway)

Environment variables consumed by the gateway container:

| Var | Default | Purpose |
|---|---|---|
| `JWT_PUBLIC_KEY` | — | RS256 public key (base64/PEM) used to verify access tokens |
| `BACKEND_URI` | `http://backend:8080` | backend upstream |
| `WATCHPARTY_URI` | `http://watch-party:8081` | watch-party upstream |
| `FRONTEND_URI` | `http://frontend:80` | frontend (SPA) upstream |
| `REDIS_HOST` | `redis-ratelimit` | Redis host for rate-limit buckets |
| `REDIS_PORT` | `6379` | Redis port |

The gateway never receives `JWT_PRIVATE_KEY` — verification only.

## 13. Source map (gateway module)

```
gateway/
├── pom.xml                         # SCG server-webmvc, oauth2-resource-server, bucket4j(+redis), lettuce
├── Dockerfile                      # multi-stage, Java 21
└── src/main/
    ├── resources/application.yml   # routes, jwt.public-key, redis host/port, server port
    └── java/com/example/gateway/
        ├── GatewayApplication.java
        ├── config/
        │   ├── SecurityConfig.java         # JwtDecoder, route/role matrix, filter wiring, 401/403 handlers
        │   └── RateLimitConfig.java        # Lettuce → Bucket4j ProxyManager (lazy), filter bean
        └── security/
            ├── GatewayJwtAuthenticationFilter.java  # opportunistic verify + X-User-* injection + inbound strip
            └── RateLimitFilter.java                 # per-user/per-IP token buckets, 429 + headers
```

## 14. What changed outside the gateway

**Backend**
- New `security/GatewayAuthenticationFilter` — trusts `X-User-*`, rebuilds the
  SecurityContext (so `@PreAuthorize` + controllers are unchanged).
- `security/JWTAuthenticationFilter` **deleted** (the backend no longer parses JWTs on
  requests). `JWTProvider` **kept** — it still signs tokens at issuance.
- `SecurityConfig` — swapped the filter; **removed CORS** (bean, config, `app.cors.*`).
- Refresh cookie flips to `SameSite=Lax`, not `Secure`, via `AUTH_COOKIE_*` env.
- No host port anymore (internal only).

**Frontend**
- `constants.jsx` — API base uses `??` so an empty value means same-origin (relative).
- `docker-entrypoint.sh` — honors an explicit empty `API_BASE_URL` as same-origin.
- `nginx.conf` — CSP `connect-src` tightened to `'self'`. nginx is now internal, behind
  the gateway (its `/ws` proxy is bypassed; the gateway owns `/ws`).
- No host port anymore (internal only).

**Compose / env**
- New `gateway` service on `8080`; `backend` and `frontend` lose their host ports.
- `FRONTEND_URL` / `CORS_ALLOWED_ORIGINS` / `BASE_URL` point at the gateway origin.

## 15. Scaling notes

Horizontal scaling of the gateway needs **no code change**: token verification is
stateless (public key), and both the rate-limit buckets and the refresh tokens already
live in Redis/Postgres, shared across instances. Watch-party is already multi-instance-ready
via its Redis pub/sub bridge; SockJS fallback transports would want sticky sessions at the
gateway if you scale it.

## 16. Known limitations / not done

- **Per-message WebSocket authentication** (REL-08) — the gateway proxies the `/ws`
  handshake but does not authenticate individual STOMP frames.
- **Trusted-header trust boundary** is the internal Docker network (no header signing /
  mTLS between gateway and backend) — appropriate for this deployment, not for a
  zero-trust internal network.

See [`tech-debt.md`](tech-debt.md) for the full backlog.
