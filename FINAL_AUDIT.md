# Cinemate — Complete Engineering Audit

**Reviewer:** Independent Staff/Principal Engineer review, merged with the original peer audit
**Scope:** `backend` (Spring Boot monolith), `watch-party` (Spring Boot microservice), `hate-api` (FastAPI/PyTorch), `frontend` (React/Vite), `compose.yaml` and supporting infrastructure
**Method:** Static code review across all four services, live verification of several claims against actual source (not just pattern-matching), and hands-on infrastructure work (getting the stack running end-to-end via Docker Compose), which surfaced at least one production-blocking defect that pure static review would not have caught.

> **Document note:** This file is a single-source merge of the original nine-document peer audit (previously `docs/audit/architecture-review.md`, `code-quality-review.md`, `findings-summary.md`, `frontend-review.md`, `hate-api-review.md`, `performance-review.md`, `reliability-review.md`, `security-review.md`, `testing-review.md`) and an independent follow-up review (previously `FINAL_AUDIT.md`). Every finding from every source document is preserved below in full — nothing was dropped, only reorganized and, where the two reviews overlapped or disagreed, reconciled with evidence (see §15, Cross-Audit Reconciliation). The original nine files and the previous standalone summary have been deleted now that their content lives here in full.

**Total findings: 91** — 53 from the original audit (IDs `ARC-`, `CQ-`, `SEC-`, `PERF-`, `REL-`, `HS-`, `FE-`, `TEST-`), 38 net-new from the independent follow-up (IDs suffixed `-NEW-` or prefixed `API-`/`DB-`).

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Overall Project Assessment](#2-overall-project-assessment)
3. [Findings Index & Severity Distribution](#3-findings-index--severity-distribution)
4. [Architecture Review](#4-architecture-review)
5. [Code Quality Review](#5-code-quality-review)
6. [API Design Review](#6-api-design-review)
7. [Security Review](#7-security-review)
8. [Performance Review](#8-performance-review)
9. [Reliability Review](#9-reliability-review)
10. [Database Review](#10-database-review)
11. [DevOps & Infrastructure Review](#11-devops--infrastructure-review)
12. [Frontend Review](#12-frontend-review)
13. [Hate-Speech Moderation Service — Deep Review](#13-hate-speech-moderation-service--deep-review)
14. [Testing & Observability Review](#14-testing--observability-review)
15. [Cross-Audit Reconciliation](#15-cross-audit-reconciliation)
16. [Original Audit Digest (Top Changes, Hotspot Files, Positive Observations)](#16-original-audit-digest-top-changes-hotspot-files-positive-observations)
17. [Prioritized Action Plan](#17-prioritized-action-plan)
18. [Learning Notes](#18-learning-notes)

---

## 1. Executive Summary

Cinemate is a well-scoped, ambitiously architected portfolio/capstone project: a Spring Boot monolith plus a Redis-backed real-time microservice plus a PyTorch content-moderation microservice plus a React SPA, wired together with Docker Compose and polyglot persistence (MySQL + MongoDB). For a project at this stage, the breadth of what has been attempted — OAuth2, WebSockets, async cascade deletes, compound Mongo indexes, Testcontainers-based integration tests, multi-stage Docker builds — is genuinely above the median for comparable projects.

It is **not production-ready**, and the gap is not cosmetic. The most serious problems cluster around three themes:

1. **Trust boundaries are inconsistently enforced.** Almost no request body in the entire backend is validated (2 of ~28 DTOs use Bean Validation, and validation failures would crash into a generic 500 anyway because `MethodArgumentNotValidException` isn't handled). The watch-party WebSocket channel accepts unauthenticated control messages from any origin. A client can inject arbitrary strings into Redis key namespaces. Verification codes are 6-digit integers with no rate limiting.
2. **Correctness under concurrency has not been considered.** Vote counts, comment counts, and watch-party participant counts are all read-modify-write instead of atomic, so they will drift under any real concurrent load — not a hypothetical, a guaranteed outcome of the current code.
3. **Operational maturity is very early-stage.** No metrics, no structured logging, no request tracing, one CI workflow (backend tests only), no dependency scanning, no migration tool (schema evolution relies on Hibernate `ddl-auto`, and the dev/prod values for it actually disagree with each other), and no backup strategy for either database.

None of this is surprising or shameful for a project at this maturity level — it is exactly the profile of a strong single/small-team build that has not yet been through a hardening pass. The purpose of this document is to be that hardening pass on paper: a concrete, prioritized list of what to fix, why, and in what order.

---

## 2. Overall Project Assessment

### Strengths

- **Ambition and breadth.** Polyglot persistence, a real-time microservice with Redis pub/sub, an ML-based moderation service, OAuth2 login, and a full React frontend is a lot of surface area to have working at all.
- **Consistent soft-delete discipline in MongoDB**, with async, batched cascade deletion (`CascadeDeletionService`) — this is a genuinely mature pattern that many production codebases get wrong.
- **Good compound-index design** on the MongoDB collections that matter most for query performance (`Post`, `Comment` feed/sort patterns).
- **Multi-stage Docker builds** on all three JVM/Python services, with non-root users and health checks on every service in `compose.yaml`.
- **A real test suite** — 57 backend test files touching nearly every module (this is *far* better than "no tests"; the problems are in test *design*, not test *existence*).
- **Environment variable hygiene is good where it's been applied**: secrets flow through `.env`/`.env.prod` files that are correctly gitignored, with a documented `docs/environment.md` and matching `.env.example` templates.
- **The `AccessService` centralization pattern** (one place that answers "can this user do this?") is the right instinct architecturally, even where its call sites have bugs.
- **Docker Compose health checks** — every service has a health check with sensible timeouts, and `depends_on: condition: service_healthy` is used correctly throughout.
- **The internal API key filter concept** (`InternalApiKeyFilter`) is structurally the right idea for service-to-service auth, even though its current defaults are wrong (see SEC-08).
- **Testcontainers usage** (`AbstractMongoIntegrationTest`) shows real awareness of proper integration-test infrastructure, not just unit mocks.
- **Batch processing in `CascadeDeletionService`** uses a `BATCH_SIZE` constant and processes deletions in chunks rather than loading everything at once.

### Weaknesses

- **Input validation is effectively absent** across the REST API surface — this is the single biggest gap in the whole codebase.
- **Concurrency correctness is an afterthought**: every counter in the system (votes, comments, watch-party participants) uses a non-atomic read-modify-write.
- **The watch-party microservice extraction has all of the operational cost of a microservice (network hop, auth surface, startup coupling) and none of the benefit** (no independent scaling, no independent data store of its own).
- **Zero production observability**: no metrics, no tracing, no structured logs, no alerting.
- **Dependency and Dockerfile drift**: the hate-api Dockerfile silently ignores its own pinned `requirements.txt`, and this is not a theoretical risk — it caused a real crash-loop that had to be fixed by hand during this engagement (§11.1).
- **CI covers one of four services.**
- **The production frontend build is non-functional out of the box** (`.env.production` bakes in `localhost` URLs — see FE-02).

---

## 3. Findings Index & Severity Distribution

### Severity distribution

| Severity | Original audit | This review (new) | Combined |
|---|---|---|---|
| 🔴 Critical | 7 | 4 | 11 |
| 🟠 High | 20 | 15 | 35 |
| 🟡 Medium | 18 | 15 | 33 |
| 🟢 Low | 8 | 4 | 12 |
| **Total** | **53** | **38** | **91** |

### Full findings index

| ID | Title | Severity | Section |
|---|---|---|---|
| ARC-01 | Unjustified dual-database split (MySQL + MongoDB) | High | §4 |
| ARC-02 | Watch-party microservice extraction is premature | Medium | §4 |
| ARC-03 | Missing API gateway / reverse proxy layer | Medium | §4 |
| ARC-04 | Verification codes stored in MySQL (timing attack vector) | High | §4 |
| ARC-05 | Three-role authentication in one service (flat role model) | Medium | §4 |
| ARC-06 | Caffeine cache is single-instance (not cluster-safe) | Medium | §4 |
| ARC-07 | `longToObjectId`/`objectIdToLong` conversion is fragile | High | §4 |
| ARC-08 | No observability infrastructure | Medium | §4 |
| ARCH-NEW-01 | No API-level input validation anywhere in the request pipeline | Critical | §4 |
| ARCH-NEW-02 | Watch-party extraction cost is even higher than measured | Medium | §4 |
| CQ-01 | Manual JSON string construction (injection risk + code smell) | High | §5 |
| CQ-02 | `e.printStackTrace()` in production code | Medium | §5 |
| CQ-03 | Mixed DI styles (`@Autowired` + `@RequiredArgsConstructor`) | Low | §5 |
| CQ-04 | Bare `RuntimeException` used everywhere (74 occurrences) | Medium | §5 |
| CQ-05 | `Verfication` typo throughout the codebase | Low | §5 |
| CQ-06 | `canVoteComment` has wrong error message | Low | §5 |
| CQ-07 | `deletePost` double-fetch then possible NPE | Medium | §5 |
| CQ-08 | `@Transactional` on read-only methods without `readOnly=true` | Low | §5 |
| CQ-09 | `maven-compiler-plugin` declared twice in `pom.xml` | Low | §5 |
| CQ-10 | `Movie.ratingCount` initialized to 1 (off-by-one bug) | Medium | §5 |
| CQ-11 | `HateSpeechScheduler` re-analyzes all comments from last 24h | Medium | §5 |
| CQ-12 | Dead/commented-out code in controllers | Low | §5 |
| CQ-NEW-01 | `GlobalExceptionHandler` leaks raw exception messages, never logs | High | §5 |
| CQ-NEW-02 | Jackson serialization conflict on `Movie.organization` | High | §5 |
| CQ-NEW-03 | JPA/Mongo entities serialized directly as API responses | High | §5 |
| CQ-NEW-04 | Inconsistent DTO suffix casing (`Dto` vs `DTO`) | Low | §5 |
| CQ-NEW-05 | Misleading log messages in `CascadeDeletionService` | Low | §5 |
| API-NEW-01 | Unbounded, unpaginated list endpoints | Critical | §6 |
| API-NEW-02 | REST convention violations: read-only endpoints as `POST` | Medium | §6 |
| API-NEW-03 | Manual try/catch in controllers bypasses `GlobalExceptionHandler` | Medium | §6 |
| SEC-01 | JWT contains embedded user data, no revocation | High | §7 |
| SEC-02 | JJWT version 0.11.5 is outdated | Medium | §7 |
| SEC-03 | Verification code enumerable (900k combos, no rate limit) | Critical | §7 |
| SEC-04 | OAuth JWT passed as a URL query parameter | High | §7 |
| SEC-05 | `allowedHeaders = List.of("*")` in CORS config | Medium | §7 |
| SEC-06 | Health endpoint path missing leading slash | Low | §7 |
| SEC-07 | hate-api has no auth, exposed on host port 8000 | High | §7 |
| SEC-08 | `InternalApiKeyFilter` fails open when key unset | High | §7 |
| SEC-09 | `OAuthSuccessHandler` has both `@Autowired` and constructor injection | Low | §7 |
| SEC-10 | Verification codes stored in plain text in MySQL | High | §7 |
| SEC-NEW-01 | Zero input validation is also a security finding | Critical | §7 |
| SEC-NEW-02 | watch-party WebSocket: any origin + client-supplied Redis key | Critical | §7 |
| SEC-NEW-03 | Internal API key comparison is not constant-time | Medium | §7 |
| SEC-NEW-04 | Health-check bypass in `InternalApiKeyFilter` uses `startsWith` | Low | §7 |
| SEC-NEW-05 | Unsanitized chat sender name rendered in watch-party UI | Medium | §7 |
| SEC-NEW-06 | hate-api Dockerfile ignores its own pinned `requirements.txt` | High | §7 |
| PERF-01 | N+1 Redis reads in `getPartyMembers()` | High | §8 |
| PERF-02 | HTTP call inside `@Transactional` holds DB connection open | High | §8 |
| PERF-03 | Caffeine cache has no invalidation on write | High | §8 |
| PERF-04 | `HateSpeechScheduler` loads all of today's comments into memory | High | §8 |
| PERF-05 | `deleteKeysByPattern()` uses Redis `KEYS` in production | High | §8 |
| PERF-06 | `AccessService.canDeleteComment()` issues up to 3 sequential queries | Medium | §8 |
| PERF-07 | `getMoviesOverview()` uses unsafe raw cast | Medium | §8 |
| PERF-08 | No database indexes on `ratingCount`, `averageRating` | Medium | §8 |
| PERF-09 | `SimpleClientHttpRequestFactory` has no connection pooling | Medium | §8 |
| PERF-NEW-01 | No HikariCP tuning; defaults undersized for this workload | Medium | §8 |
| PERF-NEW-02 | Vote read-modify-write independently rediscovered (≈REL-01) | — | §8 |
| REL-01 | Race condition on vote and comment counters | Critical | §9 |
| REL-02 | Watch-party state split between MySQL/Redis, no compensation | Critical | §9 |
| REL-03 | `addComment()` double-fetch without null check | High | §9 |
| REL-04 | Verification code not deleted after password reset | High | §9 |
| REL-05 | `forgetPassword()` returns empty object on email failure | Medium | §9 |
| REL-06 | Participant count decrement not atomic in watch-party | High | §9 |
| REL-07 | Cascade delete updates `commentCount` with wrong value | High | §9 |
| REL-08 | WebSocket control channel has no authentication | Critical | §9 |
| REL-09 | `addUser()` stores plain-text password (latent) | Critical | §9 |
| REL-NEW-01 | Precise race-window trace for watch-party join/leave | High | §9 |
| REL-NEW-02 | No graceful shutdown for watch-party | Low | §9 |
| DB-NEW-01 | `User` JPA cascade to 6 child collections (dormant landmine) | Medium | §10 |
| DB-NEW-02 | `Vote.isPost: Boolean` should be enum; index references wrong field | Medium | §10 |
| DB-NEW-03 | Inconsistent soft-delete across MongoDB collections | Medium | §10 |
| DB-NEW-04 | No connection pool tuning; `DataInitializer` not idempotent | Low | §10 |
| §11.1 | hate-api Dockerfile ignores pinned `requirements.txt` (= SEC-NEW-06) | High | §11 |
| §11.2 | `JPA_DDL_AUTO` disagrees between dev/prod env templates | Medium | §11 |
| §11.3 | CI covers exactly one of four services | Medium | §11 |
| §11.4 | No resource governance for the databases | Low | §11 |
| §11.5 | No backup/restore strategy for either database | Medium | §11 |
| §11.6 | `DataInitializer` can never run via the documented setup path | Low | §11 |
| FE-01 | JWT stored in `sessionStorage` (XSS accessible) | High | §12 |
| FE-02 | Production env file has `localhost` URLs baked in | Critical | §12 |
| FE-03 | Frontend directly connects to internal watch-party WebSocket | High | §12 |
| FE-04 | API layer files are `.jsx` but contain no JSX | Low | §12 |
| FE-05 | Duplicate API files (forum/forums, post/posts) | Medium | §12 |
| FE-06 | No Content Security Policy header | Medium | §12 |
| FE-07 | No error boundaries in the React app | Medium | §12 |
| FE-08 | No frontend tests | High | §12 |
| FE-09 | `joinRoomApi` discards all response data except `id` | Medium | §12 |
| FE-NEW-01 | Chat sender name not sanitized before render (= SEC-NEW-05) | Medium | §12 |
| FE-NEW-02 | `PostComments.jsx` is 765 lines doing five unrelated jobs | Low | §12 |
| FE-NEW-03 | `console.log` of API response data across ~17 files | Low | §12 |
| FE-NEW-04 | Search input fires an API call on every keystroke | Low | §12 |
| HS-01 | Synchronous moderation blocks every write operation | Critical | §13 |
| HS-02 | `@Transactional` held open during inference | High | §13 |
| HS-03 | Model called once per field instead of once per content unit | Medium | §13 |
| HS-04 | Comments not moderated at write time (inconsistency) | High | §13 |
| HS-05 | hate-api `app.py` is production-unready (7 sub-findings) | High | §13 |
| HS-06 | Nightly scheduler is an O(N) sequential HTTP bomb | High | §13 |
| HS-07 | Short-circuiting `\|\|` causes inconsistent moderation reasoning | Medium | §13 |
| HS-08 | Incomplete JSON escaping in HTTP body construction | Medium | §13 |
| TEST-01 | `@SpringBootTest` + `@BeforeEach` mock override anti-pattern | High | §14 |
| TEST-02 | No tests for critical security paths | High | §14 |
| TEST-03 | Only one true end-to-end integration test | Medium | §14 |
| TEST-04 | No tests for the watch-party microservice | Medium | §14 |
| TEST-05 | No observability / metrics | High | §14 |
| TEST-06 | Logging is not structured | Medium | §14 |
| TEST-07 | `CleanupScheduler` has no alerting on failure | Low | §14 |

---

## 4. Architecture Review

Cinemate is a movie-centric social platform composed of:

| Service | Tech | Role |
|---|---|---|
| `backend` | Spring Boot 3.5 / Java 17 | Core monolith: auth, users, movies, forums, posts, comments, votes, watch-party orchestration |
| `watch-party` | Spring Boot | WebSocket relay + Redis pub/sub for live party state |
| `hate-api` | FastAPI / PyTorch | Content moderation via RoBERTa model |
| `frontend` | React / Vite | SPA served via nginx |
| `mysql` | MySQL 8 | Relational data (users, movies, orgs, admins) |
| `mongodb` | MongoDB 6 | Document data (posts, comments, votes, forums) |
| `redis` | Redis 7 | Watch-party state + pub/sub channel |

### ARC-01 — Unjustified Dual-Database Split (MySQL + MongoDB)

**Severity:** High | **Effort to fix:** High

**Description.** The project uses MySQL for relational entities (User, Movie, Organization, Admin, WatchParty) and MongoDB for social content (Post, Comment, Vote, Forum). These two databases operate independently with no transaction boundary spanning both, meaning operations that touch both stores (e.g. adding a post increments `forum.postCount` in MongoDB while the user loading it queries MySQL) are never atomic.

**Evidence.**
- `PostService.addPost()` writes to MongoDB (`postRepository.save(post)`) and reads `forum` from MongoDB but `User` is in MySQL.
- `CommentService.addComment()` updates `post.commentCount` (MongoDB) and loads the user ID from a JWT claim that originated from MySQL.
- `WatchParty` entity lives in **MySQL** (`@Entity`) but all live state (members, participant count) lives in **Redis**. These two are updated in separate calls with no rollback if the Redis call fails after the MySQL commit.

**Impact.**
- Lost writes are possible: if `callMicroserviceInitialize()` throws after `watchPartyRepository.save()` commits, the MySQL record exists but Redis has no party — the party is permanently unreachable.
- Dual-database deployments double operational burden: two backup strategies, two schema migration paths, two connection pools to tune.
- No cross-store transactions means denormalized counters (`postCount`, `commentCount`, follower counts) can drift silently.

**Recommended Fix.**
1. **Short-term:** Add compensating cleanup: wrap the `create()` transaction so that if the microservice call fails, the MySQL `WatchParty` row is deleted before re-throwing.
2. **Long-term:** Evaluate consolidating to a single database. MongoDB alone can handle all data including the relational-looking entities (User, Movie). Alternatively keep MySQL but model social content there too (PostgreSQL with JSONB is often a good middle ground). The current split provides no clear benefit that could not be achieved with a single well-indexed DB.

---

### ARC-02 — Watch-Party Microservice Extraction Is Premature

**Severity:** Medium | **Effort to fix:** Medium

**Description.** The `watch-party` service was extracted as a separate Spring Boot application, but it owns no persistent data of its own (all state is ephemeral Redis), exposes no public API (network-gated), and is called **synchronously** via `RestTemplate` on every user action. The backend blocks waiting for the microservice on create, join, leave, and delete.

**Evidence.**
- `WatchPartyService.create()` calls `callMicroserviceInitialize()` inside a `@Transactional` method. If watch-party is slow, the DB transaction stays open.
- Compose file: `backend` has `depends_on: watch-party: condition: service_healthy`, meaning the entire backend refuses to start if the watch-party is unhealthy — eliminating the resilience benefit microservices are supposed to provide.
- `WatchPartyService` contains ~130 lines of HTTP boilerplate (5 near-identical `callMicroservice*` methods) that would not exist in a monolith.

**Impact.**
- Increased operational complexity with no scalability payoff (watch-party is not independently scalable in practice).
- Synchronous coupling between services defeats the independence goal of microservices.
- Startup order dependency means the whole stack fails if watch-party fails to start.

**Recommended Fix.**
**Option A (pragmatic):** Fold `watch-party` back into the backend as a `WatchPartyWebSocketHandler` package. Redis pub/sub can still be used internally. This removes 5 HTTP round-trips per operation and eliminates the inter-service authentication complexity.
**Option B (keep microservice but fix coupling):** Make calls async via an event queue (Redis Streams or a lightweight queue). The REST API call for create can still be synchronous, but leave/join/delete should be fire-and-forget with WebSocket push for confirmation.

> **Follow-up from the independent review (ARCH-NEW-02):** see the extended version of this finding after ARC-08 below — the cost of this extraction turned out to be higher than initially measured once the full `watch-party` module (WebSocket auth, Redis key validation) was reviewed.

---

### ARC-03 — Missing API Gateway / Reverse Proxy Layer

**Severity:** Medium | **Effort to fix:** Medium

**Description.** There is no API gateway in front of the backend. The frontend talks directly to `backend:8080`. Rate limiting, request routing, and TLS termination are all absent from the architecture.

**Evidence.**
- `compose.yaml` exposes `backend` on port `8080` directly.
- `frontend` nginx config proxies to the backend with no rate limiting or auth layer.
- The hate-api is exposed on host port `8000`, making it reachable from outside the Docker network.

**Impact.**
- No rate limiting means brute-force on `/api/auth/login` is unrestricted.
- hate-api port `8000` exposed to host allows direct classification bypass (submit safe text externally, post hate content internally).
- No centralized request logging at the gateway level.

**Recommended Fix.** Add nginx or Traefik as a reverse proxy with: rate limiting on auth endpoints; remove `ports: "8000:8000"` from hate-api (internal-only like watch-party); TLS termination at the edge.

---

### ARC-04 — Verification Codes Stored in MySQL (Timing Attack Vector)

**Severity:** High | **Effort to fix:** Low

**Description.** Email verification codes are stored as plain `int` in the `verifications` table in MySQL. They are 6-digit codes (100000–999999), giving 900,000 possible values. The lookup is by email only (`findByEmail`), then the code is compared in Java — not in a constant-time fashion.

**Evidence.**
- `Verfication.java` line 22: `private int code;`
- `VerificationService.verify()` line 144: `if (verifiedCode == code)` — integer equality, not constant-time.
- The table has no unique-per-attempt token; an attacker can enumerate all 900,000 codes in ~15 minutes with no rate limiting.

**Impact.** Account takeover via code enumeration. Password reset takeover for any registered email.

**Recommended Fix.**
1. Use a cryptographically random UUID token instead of a 6-digit code.
2. Store a bcrypt hash of the token, not the plain value.
3. Enforce rate limiting (max 5 attempts per email per 10 min) at the service level.
4. Consider storing verification state in Redis with a TTL instead of MySQL (avoids the cleanup scheduler entirely).

---

### ARC-05 — Three-Role Authentication in One Service (Flat Role Model)

**Severity:** Medium | **Effort to fix:** Medium

**Description.** `USER`, `ADMIN`, and `ORGANIZATION` are three separate entity classes stored in three separate tables, but all authenticate through a single `AuthenticationService` that does a role-dispatched lookup. There is no role hierarchy, no permission system — only hard-coded `hasAuthority()` checks in `SecurityConfig`.

**Evidence.**
- `AuthenticationService.findByEmailAndRole()` switches on a string role to query one of three repositories.
- `SecurityConfig` lines 63–77: 14 `requestMatchers` entries, each hard-coded to a single role string.
- `AuthenticationController` requires the client to submit `role` in the login request — meaning the frontend must know and send the user's role before authentication succeeds. This is non-standard and makes brute-force easier (try all three roles).

**Impact.**
- A user who knows an admin email can attempt login with `role=ADMIN` — the only protection is the password.
- Adding a new role requires changes to `AuthenticationService`, `SecurityConfig`, `VerificationService`, and all three `update*Password` methods — high coupling.
- Organizations cannot have admin-level access without code changes.

**Recommended Fix.** Consolidate into a single `accounts` table with a `role` discriminator column (Spring's `@DiscriminatorColumn`). Use Spring's `GrantedAuthority` hierarchy for permission checks. Remove `role` from the login request body — derive it from the account record.

---

### ARC-06 — Caffeine Cache Is Single-Instance (Not Cluster-Safe)

**Severity:** Medium | **Effort to fix:** Low

**Description.** `CacheConfig` configures Caffeine (in-process heap cache) for `exploreFeed`, `forumPosts`, and `exploreForum`. If the backend is ever scaled to multiple instances, each instance has its own independent cache — cache invalidation events on one node do not propagate to others.

**Evidence.**
- `CacheConfig.java`: `CaffeineCacheManager("exploreFeed", "forumPosts", "exploreForum")` — JVM-local.
- Redis is already deployed in the stack and used by the watch-party service.

**Impact.** Stale feeds served after posts/deletions across nodes. Not a problem today (single instance), but creates a silent correctness bug when the first scale-out happens.

**Recommended Fix.** Replace Caffeine with Spring Cache backed by Redis (`spring-boot-starter-data-redis` + `RedisCacheManager`). The infrastructure is already in place.

---

### ARC-07 — `longToObjectId` / `objectIdToLong` Conversion Is Fragile

**Severity:** High | **Effort to fix:** Medium

**Description.** The project bridges MySQL auto-increment `Long` IDs with MongoDB `ObjectId`s by encoding the `Long` as a 24-character hex string: `String.format("%024x", value)`. The reverse uses `BigInteger(hex, 16).longValue()`.

**Evidence.** `UserService.java` lines 235–242, `CommentService.java` line 122, `PostService.java` line 153, `VoteService.java` line 171, `CascadeDeletionService.java` (multiple). This conversion appears in **at least 6 different classes** (copy-pasted).

**Impact.**
- `Long` max value is `9223372036854775807` (19 digits). Formatted as hex: 16 chars, padded to 24 with leading zeros. This is a valid `ObjectId` only because MongoDB accepts any 12-byte value, but if an `ObjectId` generated by MongoDB ever needs to be converted back to a `Long`, `BigInteger.longValue()` silently truncates. Collisions are possible when values exceed 12 bytes of significant data.
- The duplication (6+ copies) means a bug fix must be applied in 6 places.

**Recommended Fix.** Extract into a single `IdConverter` utility class. Long-term, **decouple the ID systems entirely**: store the MySQL `userId` as a plain field on MongoDB documents (`ownerId: Long`) rather than converting it into a fake `ObjectId`. This avoids the conversion entirely.

---

### ARC-08 — No Observability Infrastructure

**Severity:** Medium | **Effort to fix:** Medium

**Description.** There is no metrics collection (no Micrometer export, no Prometheus), no distributed tracing (no OpenTelemetry, Zipkin, or Jaeger), and no centralized log aggregation in the stack.

**Evidence.**
- `pom.xml` has no `spring-boot-starter-actuator` dependency (only `spring-boot-starter-web`, `data-jpa`, etc.).
- `compose.yaml` has no sidecar log collector or metrics scraper.
- The only observability is per-service SLF4J logs to stdout.

**Impact.** When the system is degraded (slow hate-api, Redis lag, MySQL connection exhaustion), there is no way to diagnose it without SSH access to the container. No alerting is possible.

**Recommended Fix.**
1. Add `spring-boot-starter-actuator` with Prometheus endpoint.
2. Add a Prometheus + Grafana service in compose for local development.
3. Add structured JSON logging (Logback with `logstash-logback-encoder`).
4. Instrument key operations (hate-api call duration, microservice call latency) with `@Timed` or manual `MeterRegistry` usage.

---

### New findings from the independent review

#### ARCH-NEW-01 — No API-level input validation anywhere in the request pipeline
**Severity:** Critical | **Effort:** Medium

This is architectural, not a per-DTO nitpick, because it's systemic: of ~28 DTO classes in the backend, **exactly 1** uses Bean Validation annotations (`ForumCreationRequest`), and only 2 controller parameters anywhere are annotated `@Valid`. Verified directly:

```bash
$ grep -rn "@Valid" backend/src/main/java | wc -l
2
$ grep -rln "@NotNull\|@NotBlank\|@Size(\|@Email\|@Min(\|@Max(\|@Pattern(" backend/src/main/java | wc -l
1
```

`AddPostDto`, `CredentialsRequest`, `AddCommentDTO`, `AddAdminDTO`, `UserDataDTO`, `MovieReviewDTO` and every other write-path DTO accept null, empty, or arbitrarily long strings straight into MongoDB/MySQL and into the hate-speech classifier. And even where `@Valid` *is* used, there is no `@ExceptionHandler(MethodArgumentNotValidException.class)` (see CQ-NEW-01), so a validation failure falls through to the generic 500 handler instead of a 400 with field errors — the one working validation path degrades ungracefully.

**Why this belongs in Architecture, not just Code Quality:** there is no framework-level boundary doing this job (no `@ControllerAdvice` normalizing it, no shared base DTO, no OpenAPI-contract-first generation). It has to be retrofitted DTO-by-DTO, which is exactly the kind of work that's cheap now and expensive later.

**Recommended fix:** Add Bean Validation annotations to every write-path DTO (`@NotBlank`, `@Size`, `@Email`, `@Pattern` for role/enum-like strings), add `@Valid` to every controller parameter that accepts a body, and add a `MethodArgumentNotValidException` handler to `GlobalExceptionHandler` returning 400 with field-level errors (see CQ-NEW-01 for the snippet).

#### ARCH-NEW-02 — `watch-party` extraction cost is even higher than the original audit measured
**Severity:** Medium (confirms/extends ARC-02) | **Effort:** Medium

The existing ARC-02 finding is correct that the microservice is synchronously coupled and has a hard startup dependency. Independent review of the full `watch-party` module adds detail that strengthens the case for folding it back into the monolith (or re-architecting the boundary):

- The service has **no authentication on its most sensitive surface** (STOMP `/ws` control channel, `setAllowedOriginPatterns("*")` — see SEC-NEW-02).
- Its own REST layer accepts a **client-supplied `partyId`** with no format validation (`WatchPartyService.java:42-44`, `StringUtils.hasText(request.getPartyId()) ? request.getPartyId() : UUID.randomUUID().toString()`), which becomes a Redis key segment — see SEC-NEW-02.
- Every state mutation (join/leave) is a **multi-step, non-atomic Redis read-modify-write** (REL-06, confirmed independently with an even more precise race-window trace — see REL-NEW-01).
- It has **essentially zero test coverage** (one placeholder context-load test for a module with real concurrency and security surface — see TEST-04).

None of this is a reason the service *shouldn't* exist — real-time fan-out genuinely benefits from Redis pub/sub — but as currently built it is a second attack surface and a second source of truth with strictly worse guarantees than the monolith it was split from, for no scaling benefit realized yet. **Recommendation stands as ARC-02 Option A** (fold back into the monolith as an internal package, keep Redis for pub/sub only) **unless there is a concrete near-term plan to scale it independently** — in which case, fix REL-NEW-01/SEC-NEW-02/SEC-NEW-03 first, because right now the "microservice" is the least safe part of the system.

---

## 5. Code Quality Review

The backend is a Spring Boot monolith with ~24 packages. Overall structure is clean, but there are recurring patterns of poor exception handling, duplicated utility code, mixed injection styles, and a few critical logic bugs.

### CQ-01 — Manual JSON String Construction (Injection Risk + Code Smell)

**Severity:** High | **Effort to fix:** Low

**Description.** `HateSpeechService.analyzeText()` builds the JSON request body by manual string concatenation with a partial escape attempt.

**Evidence.**
```java
// HateSpeechService.java line 34
String body = "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
```

**Impact.** The escaping is **incomplete**: newlines (`\n`), carriage returns (`\r`), and other control characters are not escaped. A comment containing a literal newline breaks the JSON, causing the hate-speech call to fail and silently allowing the content through (fail-open). This is a code smell: the project already uses Jackson (via Spring Boot) — there is no reason to build JSON by hand.

**Recommended Fix.**
```java
private static final ObjectMapper MAPPER = new ObjectMapper();

public boolean analyzeText(String text) {
    Map<String, String> body = Map.of("text", text);
    HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
    ...
}
```

---

### CQ-02 — `e.printStackTrace()` in Production Code

**Severity:** Medium | **Effort to fix:** Low

**Description.** `VerificationService` uses `e.printStackTrace()` in three catch blocks instead of the SLF4J logger that is available project-wide.

**Evidence.**
```java
// VerificationService.java lines 153, 192, 242
} catch (Exception e) {
    e.printStackTrace();   // ← writes to stderr, not structured logs
    return Optional.empty();
}
```

**Impact.** Stack traces go to raw stderr, bypassing the logging framework. In containerized environments this output is unstructured and hard to correlate with other log lines. The exception is swallowed after printing — callers get `Optional.empty()` or `false` with no context.

**Recommended Fix.** Replace with `log.error("Verification failed for {}", email, e)`. The class already imports Lombok — add `@Slf4j`.

---

### CQ-03 — Mixed DI Styles (`@Autowired` + `@RequiredArgsConstructor`)

**Severity:** Low | **Effort to fix:** Low

**Description.** `UserService` and `VerificationService` mix constructor injection (via `@RequiredArgsConstructor`) with field injection (`@Autowired`), making it impossible to use the Lombok-generated constructor to inject the `@Autowired` fields.

**Evidence.**
```java
// UserService.java
@RequiredArgsConstructor          // generates constructor for final fields
public class UserService {
    private final UserRepository userRepository;  // injected via constructor ✓
    @Autowired
    private VerificationService verificationService;  // field injection ✗
    @Autowired
    private JWTProvider jwtProvider;                  // field injection ✗
```

**Impact.** Makes the class untestable without Spring context or `ReflectionTestUtils` hacks (which the tests confirm — `UserServiceTest` line 58 uses `ReflectionTestUtils.setField`). Hides actual dependencies from the constructor signature.

**Recommended Fix.** Make all dependencies `final` and remove `@Autowired`. If circular dependencies exist (the reason `@Lazy` appears on `OAuthSuccessHandler`), resolve the cycle architecturally rather than with `@Lazy`.

---

### CQ-04 — Bare `RuntimeException` Used Everywhere (74 occurrences)

**Severity:** Medium | **Effort to fix:** Medium

**Description.** The codebase throws `RuntimeException` directly in at least 74 places. This makes it impossible to differentiate "entity not found" (400/404) from "unexpected server error" (500) without string matching on the message.

**Evidence.**
```java
// WatchPartyService.java (repeated pattern)
.orElseThrow(() -> new RuntimeException("Movie not found"));
.orElseThrow(() -> new RuntimeException("User not found"));
.orElseThrow(() -> new RuntimeException("Watch party not found"));
```
The global error handler (`errorHandler` package exists) cannot distinguish these from genuine 500s.

**Impact.** The watch-party controller catches all `RuntimeException` and returns `400 Bad Request` — a "party not found" scenario incorrectly gets a 400 instead of a 404. Monitoring/alerting cannot distinguish operational errors from bugs.

**Recommended Fix.** Define typed exceptions: `EntityNotFoundException extends RuntimeException`, `BusinessRuleViolationException`, etc. Map them to HTTP status codes in a `@ControllerAdvice`. Spring already provides `ResponseStatusException` as a quick intermediate step. *(See CQ-NEW-01 for the fully worked `GlobalExceptionHandler` this feeds into.)*

---

### CQ-05 — `Verfication` Typo Throughout the Codebase

**Severity:** Low | **Effort to fix:** Low

**Description.** The entity class, repository, and DTO are all named `Verfication` (missing an 'i') instead of `Verification`.

**Evidence.** `Verfication.java`, `VerificationRepository.findByEmail()` returns `Optional<Verfication>`, etc.

**Impact.** Cosmetic but signals rushed development. Any external tooling (code generators, OpenAPI) will expose the typo in generated names.

**Recommended Fix.** Rename `Verfication` → `Verification` across the codebase (IDE refactor, low risk).

---

### CQ-06 — `canVoteComment` Has Wrong Error Message

**Severity:** Low | **Effort to fix:** Low

**Description.** The error message in `VoteService.canVoteComment()` says "Post not found" when it is looking up a comment.

**Evidence.**
```java
// VoteService.java line 112
throw new IllegalArgumentException("Post not found with id: " + commentId);
//                                   ^^^^ should be "Comment"
```

**Impact.** Misleading error message makes debugging harder. Not a runtime bug, but a quality indicator.

**Recommended Fix.** `"Comment not found with id: " + commentId`.

---

### CQ-07 — `deletePost` Has Null-Check After `canDeletePost` Already Fetches the Entity

**Severity:** Medium | **Effort to fix:** Low

**Description.** `PostService.deletePost()` calls `accessService.canDeletePost()` (which fetches the post from MongoDB), then fetches it **again** with `mongoTemplate.findById()`. If the post was deleted between the two calls, the second fetch returns null and the method throws NPE on the next line.

**Evidence.**
```java
// PostService.java lines 108-120
public void deletePost(ObjectId postId, Long userId) {
    if (!accessService.canDeletePost(...))  // fetch #1
        throw ...;
    Post post = mongoTemplate.findById(postId, Post.class);  // fetch #2
    if (post == null)   // too late — forum.setPostCount crashes first if null
        throw ...;
    Forum forum = mongoTemplate.findById(post.getForumId(), Forum.class);
    forum.setPostCount(forum.getPostCount() - 1);   // NPE if forum is null
```

**Impact.** Double read (wasted I/O). If the forum is deleted between the post fetch and the forum fetch, `forum.setPostCount()` throws NPE, resulting in an unhandled 500.

**Recommended Fix.** Pass the already-fetched `Post` object from `canDeletePost()` (change return type to `Optional<Post>`) or add null checks for `forum`.

---

### CQ-08 — `@Transactional` on Read-Only Methods Without `readOnly=true`

**Severity:** Low | **Effort to fix:** Low

**Description.** Many service methods annotated `@Transactional` are read-only operations (getters) but do not declare `readOnly = true`. This prevents Hibernate from applying read-only optimizations (no dirty checking, no snapshot caching).

**Evidence.**
```java
// CommentService.java
@Transactional
public Page<Comment> getPostComments(...) { ... }  // read-only

@Transactional
public List<Comment> getReplies(...) { ... }       // read-only
```

**Impact.** Minor performance overhead for all read calls (unnecessary dirty checking).

**Recommended Fix.** Use `@Transactional(readOnly = true)` on all read-only methods.

---

### CQ-09 — `maven-compiler-plugin` Declared Twice in `pom.xml`

**Severity:** Low | **Effort to fix:** Low

**Description.** `pom.xml` declares `maven-compiler-plugin` twice (lines 130–140 and 175–187) with slightly different configurations (the second adds a `<version>` to the Lombok path).

**Evidence.**
```xml
<!-- pom.xml lines 130 and 175 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    ...
</plugin>
<!-- ...30 lines later... -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    ...
</plugin>
```

**Impact.** Maven will merge the two declarations unpredictably; the second one wins for conflicting keys. The first declaration (without `<version>`) may be silently ignored. *(Confirmed directly in `backend/pom.xml` during the independent review — still present.)*

**Recommended Fix.** Merge into a single `maven-compiler-plugin` declaration.

---

### CQ-10 — `Movie.ratingCount` Initialised to 1 (Off-by-One Bug)

**Severity:** Medium | **Effort to fix:** Low

**Description.** The `@PrePersist` hook on `Movie` initializes `ratingCount = 1` instead of `0`, inflating the average rating when there are no reviews yet.

**Evidence.**
```java
// Movie.java lines 81-83
if (ratingCount == null) {
    ratingCount = 1;  // ← should be 0
}
```

**Impact.** A movie with one 5-star review reports an average of `5 / 2 = 2.5` instead of `5.0`. Data integrity bug affecting the core movie-rating feature.

> **Independently re-verified during the follow-up review (see §15.3).** A parallel automated reviewer initially claimed this was *not* a real bug, reasoning that `MovieReviewService.addOrUpdateReview()` guards with `if (movie.getRatingCount() == null) movie.setRatingCount(0)`. Tracing the actual data flow by hand shows that guard never fires — because `@PrePersist` already set `ratingCount` to the non-null value `1` before any review exists, so by the time the guard runs the field is `1`, not `null`. The first real review then computes `ratingCount = 1 + 1 = 2` and `averageRating = ratingSum / 2` instead of `/ 1`, reproducing exactly the `5-star review shows as 2.5` bug described here. **This finding is confirmed accurate.**

**Recommended Fix.** `ratingCount = 0;`. Ensure `averageRating` calculation guards against division by zero.

---

### CQ-11 — `HateSpeechScheduler` Re-Analyzes All Comments From the Last 24 Hours

**Severity:** Medium | **Effort to fix:** Medium

**Description.** The nightly scheduler fetches **all** comments created in the last 24 hours and re-runs them through the hate-speech model individually, one HTTP call per comment.

**Evidence.**
```java
// HateSpeechScheduler.java
List<Comment> todayComments = commentRepository.findAllByCreatedAtBetween(yesterday, now);
for (Comment comment : todayComments) {
    cleanComment(comment);  // → hateSpeechService.analyzeText() → HTTP call
}
```

**Impact.** O(N) HTTP calls to hate-api at midnight. At 10,000 comments/day, this is 10,000 sequential HTTP calls (10s read timeout each → potentially 27 hours). Comments were already moderated at write time. Re-scanning them is duplicated work unless the model changes. No batching, no parallelism, no page-size limit on the DB query (full collection scan). *(See HS-06 for the full quantitative breakdown of this same defect.)*

**Recommended Fix.**
1. Add an `isModerated: Boolean` flag to `Comment` and only re-scan unmoderated comments.
2. If re-scanning is needed for model updates, add a `@Scheduled` job that processes in pages with a configurable batch size and uses a thread pool for parallel HTTP calls.
3. Consider queueing flagged content rather than re-scanning all.

---

### CQ-12 — Dead/Commented-Out Code in Controllers

**Severity:** Low | **Effort to fix:** Low

**Description.** Several controllers contain commented-out `System.out.println` debug statements.

**Evidence.**
```java
// LikedMovieController.java line 23
//        System.out.println("userId = " + userId);

// WatchHistoryController.java line 27
//        System.out.println("userId = " + userId);

// WatchLaterController.java line 26
//        System.out.println("userId = " + userId);
```

**Impact.** Dead code; signals the debugging practice was `System.out` rather than a proper logger, and it was committed to version control.

**Recommended Fix.** Delete the commented lines.

---

### New findings from the independent review

#### CQ-NEW-01 — `GlobalExceptionHandler` leaks raw exception messages and never logs
**Severity:** High | **Category:** Error Handling / Information Disclosure | **File:** `backend/src/main/java/org/example/backend/errorHandler/GlobalExceptionHandler.java:14-25`

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiError> handleException(Exception ex, HttpServletRequest request) {
    ApiError error = ApiError.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message(ex.getMessage())   // <-- raw exception message returned to the client
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build();
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
}
```

Two separate problems in eight lines:

1. **Every unhandled exception's `getMessage()` is returned verbatim to the HTTP client.** For a `DataIntegrityViolationException` this can include SQL fragment/constraint names; for a `NullPointerException` under modern JDKs it can include internal field/method names ("Cannot invoke because `x.y` is null"); for a MongoDB driver exception it can include connection details. This is textbook information disclosure (OWASP API8:2023 — Security Misconfiguration).
2. **Nothing is logged.** There is no `log.error(...)` call anywhere in this class. Combined with the complete absence of observability infrastructure (ARC-08/TEST-05), this means **every 500 error in production is invisible** except to whichever client happened to receive it. This is worse than "no metrics" — it's "no record exists that the error occurred at all."

It also only handles `Exception` and `IllegalArgumentException` — see API-NEW-03 for the missing-handler list (`MethodArgumentNotValidException`, `IllegalStateException`, `AccessDeniedException`, `DataIntegrityViolationException`).

**Recommended fix:**
```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        ApiError error = ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred")   // generic, safe message
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiError.builder()
                .status(400).message(detail).path(request.getRequestURI())
                .timestamp(LocalDateTime.now()).build());
    }
    // + IllegalStateException, AccessDeniedException, DataIntegrityViolationException handlers
}
```

#### CQ-NEW-02 — Jackson serialization conflict on `Movie.organization` will break the public movie-browse endpoint
**Severity:** High | **Category:** Correctness / API Design | **File:** `backend/src/main/java/org/example/backend/movie/Movie.java:62-74`

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "organization_id", nullable = false)
@JsonIgnoreProperties({"id", "email", "password", "about", "createdAt", "releasedMovies"})
private Organization organization;              // field serializes under key "organization"

@JsonProperty("organization")
public String getOrganizationName() {           // method ALSO serializes under key "organization"
    return organization != null ? organization.getName() : null;
}
```

Two different accessors — a field and a derived getter — are both mapped to the same JSON property name `"organization"`. Unlike the `admin` field two lines above (correctly hidden with `@JsonIgnore`), the `organization` field itself is never hidden; only *its nested properties* are filtered via `@JsonIgnoreProperties`, which has no effect on the name collision. Depending on Jackson's introspection order this either throws `InvalidDefinitionException: Conflicting getter definitions for property "organization"` at serialization time, or silently and non-deterministically picks one accessor over the other across Jackson versions — neither outcome is acceptable.

This is not a cold corner of the code: `MovieController.getSpecificMovie()` returns `ResponseEntity<Movie>` directly, and `/api/movie/**` is `permitAll()` in `SecurityConfig` — i.e. this is the **public movie detail/browse endpoint**, one of the most-used routes on the platform.

**Recommended fix:**
```java
@JsonIgnore
private Organization organization;

@JsonProperty("organizationName")   // give the derived value its own name
public String getOrganizationName() {
    return organization != null ? organization.getName() : null;
}
```
(This is a stopgap; the real fix is CQ-NEW-03 below — stop serializing entities at all.)

#### CQ-NEW-03 — JPA/Mongo entities are serialized directly as API responses instead of DTOs
**Severity:** High | **Category:** API Design / Encapsulation

At least 9 endpoints return `@Entity`/MongoDB document classes directly: `MovieController.searchMoviesPost()` → `Page<Movie>`, `MovieController.getSpecificMovie()` → `Movie`, `AdminController.getRequestedMovie()` → `Movie`, `OrganizationController.getOrganizationMovies()` → `List<Movie>`, `PostController` → `Page<Post>`/`Post`, `CommentController` → `Page<Comment>`/`List<Comment>`, `ForumController.createForum()` → `Forum`. The project already has the right pattern in places (`MovieView`, `PostView`, `LikedMovieView` interface projections) — the problem is inconsistency, not ignorance of DTOs.

Returning entities directly means: (a) any `FetchType.LAZY` relationship touched during serialization throws `LazyInitializationException` if outside a transaction, (b) internal schema changes become breaking API changes with no seam to prevent it, (c) it's how CQ-NEW-02 above was able to happen in the first place — entity-level `@JsonIgnore`/`@JsonProperty` annotations are fighting a losing battle against the entity's real job (persistence mapping).

**Recommended fix:** Standardize on the `*View`/`*ResponseDTO` pattern already used elsewhere; make it the rule, not the exception, via a lint/review checklist item ("no `@RestController` method may return `@Entity` or a Spring Data `Document` class").

#### CQ-NEW-04 — Inconsistent DTO suffix casing (`Dto` vs `DTO`)
**Severity:** Low | **Category:** Consistency

`AddPostDto`, `MovieRequestDTO`, `AddAdminDTO`, `UserDataDTO` — mixed casing across ~28 DTO classes. Cosmetic, but it's friction on every "find the DTO for X" search. Pick one (the project already leans `DTO` in the majority) and rename via IDE refactor.

#### CQ-NEW-05 — Misleading log messages in `CascadeDeletionService`
**Severity:** Low | **Category:** Observability | **File:** `backend/src/main/java/org/example/backend/deletion/CascadeDeletionService.java:136,178,186`

Several log lines say `"Found {} posts to delete for forum {}"` when the method is actually counting `following` documents, not posts (copy-paste from an earlier, more specific method into a generic helper). Harmless today, actively misleading the next time someone is debugging a deletion-related incident at 2am from logs alone (which, per CQ-NEW-01/ARC-08, is the *only* debugging tool available in this system). Parameterize the log message with the actual collection name.

---

## 6. API Design Review

*(New top-level section — the original audit touched API design tangentially through SEC-06's missing leading slash and FE-09's wasted response data, but never assessed it holistically. Independent review found this deserves to be a first-class category.)*

#### API-NEW-01 — Unbounded, unpaginated list endpoints
**Severity:** Critical | **Category:** Performance / Availability | **Files:** `AdminController.java:27,59,73-76`, `OrganizationController.java:73-76,85-89`, `RequestsRepository.java:24-27`

`AdminController.findAllAdminRequests()`, `findAllPendingRequests()`, `OrganizationController.getOrgRequests()`, and `getOrganizationMovies()` all return a full `List<T>` with no `Pageable`/limit. This is the same failure mode as PERF-04 (nightly scheduler loads all of today's comments) but on the hot path of an admin/org dashboard, not a nightly job — and it's directly reachable by an authenticated ADMIN/ORGANIZATION user at any time. As request/movie volume grows this becomes an unbounded-memory, unbounded-response-size endpoint — the textbook definition of a resource-exhaustion vector, and it doesn't even require a malicious actor to trigger, just organic growth.

**Recommended fix:** every list-returning endpoint takes `Pageable` (with `@PageableDefault(size = 20)` and a **max page size cap** — Spring's `Pageable` does not cap size by default, so also configure `spring.data.web.pageable.max-page-size`).

#### API-NEW-02 — REST convention violations: read-only endpoints exposed as `POST`
**Severity:** Medium | **Category:** API Design | **Files:** `AdminController.java` (`/v1/find-admin-requests`, `/v1/get-pending-requests`), `MovieController.java` (`/v1/get-specific-movie-overview`), `OrganizationController.java` (`/v1/get-organization-movies`) — 12+ occurrences

Read-only lookups implemented as `@PostMapping`. Beyond the RFC 7231 semantic violation, this has a concrete cost: these responses are never cache-eligible at any layer (browser, CDN, reverse proxy) because caches don't cache POST by default, and it makes the API surface unpredictable for API consumers/tooling (OpenAPI generators, Postman collections, integration testers all assume GET-for-reads). This is inconsistent with the fact that `/api/movie/**` search *does* correctly differentiate (though it's also `POST` for search, which is defensible for a complex filter body — the admin/org endpoints above have no such excuse, they take no meaningful body).

**Recommended fix:** convert to `@GetMapping` with `@RequestParam`/`@PathVariable` where the request has no meaningful body; keep `POST` only where the request genuinely represents a complex query body that doesn't fit in a query string.

#### API-NEW-03 — Manual try/catch in controllers bypasses `GlobalExceptionHandler`, producing inconsistent error shapes
**Severity:** Medium | **Category:** API Consistency

Example (`AdminController.java`):
```java
try {
    adminService.declineRequest(userId, requestId);
    return ResponseEntity.ok("Request " + requestId + " declined successfully...");
} catch (RuntimeException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Failed to decline request " + requestId + ": " + e.getMessage());
}
```
This returns a bare `String` body on error, while `GlobalExceptionHandler`'s path returns a structured `ApiError` JSON object. A frontend consuming this API has to handle two different error response shapes depending on which controller method happened to add its own try/catch. This compounds CQ-04 (bare `RuntimeException`) — once typed exceptions exist and are handled centrally, these local catch blocks should be deleted, not fixed in place.

---

## 7. Security Review

### SEC-01 — JWT Token Contains User Data Embedded as Claims (No Revocation)

**Severity:** High | **Effort to fix:** Medium

**Description.** The JWT tokens embed `id`, `email`, `name`, `role`, and `profileComplete` as claims and have a 24-hour expiry. There is no token blacklist, no refresh token mechanism, and no way to invalidate a token before it expires.

**Evidence.**
```java
// JWTProvider.java lines 25-45
claims.put("id", account.getId());
claims.put("email", account.getEmail());
claims.put("name", account.getName());
claims.put("role", account.getRole());
// ...
long jwtExpiration = 24 * 60 * 60 * 1000;   // hardcoded 24 hours
```

**Impact.** If a user's account is banned or deleted, their existing JWT tokens remain valid for up to 24 hours. If a token is stolen (XSS, network intercept), it cannot be revoked. The `name` and `role` embedded in the token become stale — if an admin changes a user's role, the token still grants the old role for up to 24 hours.

**Recommended Fix.**
1. Shorten access token expiry to 15–30 minutes and introduce refresh tokens stored in the database (with revocation capability).
2. Alternatively, maintain a Redis-backed token blacklist for explicit revocation (simpler to implement).
3. Remove mutable user data (`name`) from token claims — derive it from the database on profile requests.

---

### SEC-02 — JJWT Version 0.11.5 Is Outdated

**Severity:** Medium | **Effort to fix:** Low

**Description.** The project uses `io.jsonwebtoken:jjwt-api:0.11.5`. The current stable release is `0.12.x`, which includes security fixes and deprecates `SignatureAlgorithm` (the deprecated enum is used in the code).

**Evidence.**
```xml
<!-- pom.xml lines 58-74 -->
<artifactId>jjwt-api</artifactId>
<version>0.11.5</version>
```
```java
// JWTProvider.java line 44
.signWith(getSigningKey(), SignatureAlgorithm.HS512)
//                         ^^^ deprecated in 0.12.x
```
*(Independently confirmed directly in `backend/pom.xml` during the follow-up review — still `0.11.5`.)*

**Impact.** Dependency on a deprecated API; migration will be needed eventually. `0.11.5` was released in 2022 and may have known CVEs by now.

**Recommended Fix.** Upgrade to `0.12.6+` and update the signing call to the new API:
```java
.signWith(getSigningKey())  // algorithm inferred from key type in 0.12.x
```

---

### SEC-03 — Verification Code Is Enumerable (900,000 Combinations, No Rate Limit)

**Severity:** Critical | **Effort to fix:** Low

**Description.** Email verification and password reset codes are 6-digit integers (100000–999999). There is no rate limiting on the verification endpoint, allowing an attacker to enumerate all 900,000 codes.

**Evidence.**
```java
// UserService.java line 99
int code = 100000 + random.nextInt(900000);
```
```java
// VerificationController.java (no rate limiting, no lockout after N failures)
@PostMapping("/verify")
public ResponseEntity<?> verifyEmail(@RequestBody VerificationDTO verificationDTO) {
    ...
}
```

**Impact.** An attacker who knows a target email can complete registration or reset their password by guessing the code. At 100 requests/second (no throttle), all codes are tried in ~2.5 hours.

**Recommended Fix.**
1. Replace the 6-digit code with a 256-bit random token (UUID or `SecureRandom`).
2. Add attempt rate limiting: lock the verification for 10 minutes after 5 failed attempts.
3. Expire codes after 10 minutes (already done by the cleanup scheduler — confirm it runs reliably).

---

### SEC-04 — OAuth JWT Passed as a URL Query Parameter

**Severity:** High | **Effort to fix:** Low

**Description.** After Google OAuth login, the backend redirects to `${frontendUrl}/oauth2/redirect?token=<JWT>`. The JWT is in the URL query string, which means it appears in browser history, server access logs, proxy logs, and HTTP `Referer` headers.

**Evidence.**
```java
// OAuthSuccessHandler.java lines 67-71
String redirectUrl = UriComponentsBuilder
        .fromUriString(frontendUrl + "/oauth2/redirect")
        .queryParam("token", token)   // ← JWT in URL
        .build()
        .toUriString();
```

**Impact.** JWT tokens leaked in access logs on any intermediary (load balancer, CDN, analytics services). Browser history exposes the token to other users of the same device. The `Referer` header on subsequent navigation can expose the token to third-party scripts.

**Recommended Fix.** Use a short-lived (30 s) one-time `state` code in the URL. Exchange it for the JWT via a subsequent POST to `/api/auth/token?code=<state>`. The JWT itself is then delivered in the response body and stored in memory or `HttpOnly` cookie.

---

### SEC-05 — `allowedHeaders = List.of("*")` in CORS Config

**Severity:** Medium | **Effort to fix:** Low

**Description.** The CORS configuration allows all request headers with no restriction.

**Evidence.**
```java
// SecurityConfig.java line 111
configuration.setAllowedHeaders(List.of("*"));
```
*(Independently re-verified: `SecurityConfig.corsConfigurationSource()` confirms `List.of("*")` alongside `setAllowCredentials(true)`.)*

**Impact.** While CORS header wildcards are not themselves a vulnerability, combined with `allowCredentials(true)` it increases exposure. If a custom header carrying internal state is added in future, it will automatically be accepted cross-origin. Best practice with `allowCredentials(true)` is to enumerate specific allowed headers.

**Recommended Fix.**
```java
configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
```

---

### SEC-06 — Health Endpoint Not Secured

**Severity:** Low | **Effort to fix:** Low

**Description.** The health endpoint path in `SecurityConfig` is missing a leading slash, meaning it may not match correctly and could either be unreachable or accidentally match other paths.

**Evidence.**
```java
// SecurityConfig.java line 62
.requestMatchers("api/health/**").permitAll()
//               ^^^ missing leading slash — should be "/api/health/**"
```
Compare with line 59: `.requestMatchers("/api/auth/**")` — has the slash.

> **Independently re-verified.** The full `SecurityConfig.securityFilterChain()` was re-read during the follow-up review. The same pattern (missing leading slash) also appears a second time on the line `.requestMatchers("api/post/**")` (line 71), which is followed a few lines later by a correctly-slashed duplicate `.requestMatchers("/api/post/**")` (line 77). Because Spring Security's `requestMatchers` typically requires a leading slash to match an actual request path (which always starts with `/`), the slash-less entries are effectively **dead rules** — harmless here only because a correctly-formed duplicate happens to exist immediately after, but the same mistake on `api/health/**` has no such duplicate to save it, which is exactly why this finding matters.

**Impact.** The health endpoint may require authentication in some Spring Security versions (path matching may fail without the leading slash), breaking Docker health checks. Or it may match unexpected paths if Spring normalizes it — a security misconfiguration.

**Recommended Fix.** `"/api/health/**"` (add the leading slash). Also remove the redundant slash-less `"api/post/**"` duplicate entry while fixing this.

---

### SEC-07 — `hate-api` Has No Authentication and Is Exposed on Host Port 8000

**Severity:** High | **Effort to fix:** Low

**Description.** The hate-api service is exposed on host port `8000` via `ports: "8000:8000"` and has no API key or authentication. Any process on the host machine can query it directly, bypassing the backend's content moderation flow entirely.

**Evidence.**
```yaml
# compose.yaml lines 52-53
ports:
  - "8000:8000"
```
```python
# hate-api/app.py — no auth header validation anywhere
@api_router.post("/analyze")
def analyze(request: TextRequest):
    print(request.text)
    return analyze_text(request.text)
```

**Impact.** An attacker on the same host can directly call `/api/hate/v1/analyze` to understand the moderation model's blind spots. The `print(request.text)` on line 43 logs every analyzed string to stdout — a privacy leak for user content.

**Recommended Fix.**
1. Remove the `ports` entry from hate-api in compose (make it internal-only like `watch-party`).
2. Add an API key header check in `app.py` (or a FastAPI dependency).
3. Remove `print(request.text)`.

---

### SEC-08 — `InternalApiKeyFilter` Fails Open When `WATCHPARTY_KEY` Is Not Set

**Severity:** High | **Effort to fix:** Low

**Description.** `InternalApiKeyFilter` explicitly skips validation if `expectedKey` is blank:
```java
if (expectedKey == null || expectedKey.isBlank()) {
    log.warn("WATCHPARTY_KEY is not configured — internal API key validation is disabled");
    filterChain.doFilter(request, response);  // ← allows all requests
    return;
}
```

**Evidence.** `InternalApiKeyFilter.java` lines 43–47. If the `WATCHPARTY_KEY` environment variable is not set in a deployment, the watch-party service becomes fully open to any request.

**Impact.** Misconfigured deployments have zero inter-service security. This is a fail-open design where fail-closed would be safer.

**Recommended Fix.** Change to fail-closed: if the key is not configured, reject the request with a 503 and a startup warning:
```java
if (expectedKey == null || expectedKey.isBlank()) {
    log.error("WATCHPARTY_KEY is not configured — rejecting all requests for safety");
    response.setStatus(503);
    response.getWriter().write("{\"error\":\"Service misconfigured\"}");
    return;
}
```
Add a Spring `@PostConstruct` that validates the key is set at startup. *(Pair this with SEC-NEW-03 below — the comparison itself should also become constant-time while this is being touched.)*

---

### SEC-09 — `OAuthSuccessHandler` Has Both `@Autowired` and Constructor Injection

**Severity:** Low | **Effort to fix:** Low

**Description.** `OAuthSuccessHandler` declares `@Autowired` on `jwtProvider` as a field, but also has a constructor that accepts `jwtProvider` as a parameter. Spring will call the constructor first, then `@Autowired` the field again — the field injection wins, but the constructor injection is dead code.

**Evidence.**
```java
// OAuthSuccessHandler.java lines 22-38
@Autowired
private JWTProvider jwtProvider;             // field injection

public OAuthSuccessHandler(JWTProvider jwtProvider, ...) {
    this.jwtProvider = jwtProvider;          // constructor injection (dead)
```

**Impact.** Minor code smell; not a runtime bug but indicates confusion about which injection wins.

**Recommended Fix.** Remove the `@Autowired` field annotation and keep only constructor injection (make field `private final`).

---

### SEC-10 — Verification Codes Are Stored in Plain Text in MySQL

**Severity:** High | **Effort to fix:** Low

**Description.** The `verifications` table stores the raw integer verification code as a column. If the database is compromised, all pending codes are exposed.

**Evidence.**
```java
// Verfication.java line 22
@Column(name = "code")
private int code;
```

**Impact.** An attacker with read access to the `verifications` table can immediately complete account takeovers or password resets for any pending verification.

**Recommended Fix.** Store a HMAC or BCrypt hash of the code. On verification, hash the submitted code and compare. This limits the damage of a database breach to offline brute-force of a 6-digit number (still weak — see SEC-03 for the proper fix of using a random UUID token instead).

---

### New findings from the independent review

#### SEC-NEW-01 — Zero input validation is also a security finding, not just a quality one
See ARCH-NEW-01. Worth restating here explicitly: unvalidated `String` fields flowing into MongoDB queries, into the hate-speech HTTP call (which itself has incomplete JSON escaping per CQ-01/HS-08), and into JPA entities is the precondition for most of the injection-adjacent risk in this system, even though no single injectable query was found (Spring Data's parameterized queries protect against classic SQL injection). Treat validation as a security control, not a UX nicety — it's the outermost layer of defense-in-depth for everything downstream.

#### SEC-NEW-02 — `watch-party` WebSocket accepts any origin *and* a client-supplied Redis key segment
**Severity:** Critical | **Category:** Authentication / Injection | **Files:** `watch-party/.../websocket/WebSocketConfig.java:23-25`, `watch-party/.../WatchPartyService.java:42-44`

This extends REL-08 (already flagged as Critical — no auth on the STOMP control channel) with two compounding facts verified independently:

1. `WebSocketConfig` sets `.setAllowedOriginPatterns("*")` — any origin, not just the frontend's, can open the WebSocket. Combined with no message-level auth, this is a textbook Cross-Origin WebSocket Hijack: a page on `evil.example` can open a connection to the watch-party socket and inject `PLAY`/`PAUSE`/`SEEK`/chat events into any party whose ID it can guess or scrape.
2. **`partyId` is not server-generated — it's client-suppliable**, verified directly:
   ```java
   // WatchPartyService.java:42-44
   String partyId = StringUtils.hasText(request.getPartyId())
           ? request.getPartyId()
           : UUID.randomUUID().toString();
   ```
   The original audit's note that "`partyId` is a UUID so guessing it is hard" (see REL-08) is **only true if the client doesn't supply one**, and nothing stops it from supplying one. `partyId` becomes a raw segment in Redis keys (`party:{partyId}:members`, etc.) with **no format validation** (`@Pattern`, length cap). A client can submit `partyId = "*"` or a colon-laden string and pollute the Redis key namespace, potentially colliding with or shadowing another party's keys, or (in combination with the `KEYS`/pattern-based deletion in `RedisService`, PERF-05) causing unintended cross-party deletions.

**Recommended fix:** (a) restrict `setAllowedOriginPatterns` to the actual frontend origin(s), sourced from the same `CORS_ALLOWED_ORIGINS`-style env var already used elsewhere; (b) validate a JWT on STOMP `CONNECT` as REL-08 already recommends; (c) add `@Pattern(regexp = "^[a-zA-Z0-9_-]{1,64}$")` to the party-creation request's `partyId` field, or simplest of all, **stop accepting a client-supplied `partyId` at all** — always server-generate it, which also closes the injection vector entirely with zero validation code needed.

#### SEC-NEW-03 — Internal API key comparison is not constant-time
**Severity:** Medium | **Category:** Cryptography | **File:** `watch-party/.../security/InternalApiKeyFilter.java:50`

```java
if (!expectedKey.equals(providedKey)) { ... }
```
`String.equals()` short-circuits on the first mismatched character, making the comparison time vary with how many leading characters match — a classic (if low-probability-of-exploitation-in-practice, given this is an internal service-to-service call) timing side channel. Low effort, correct-by-construction fix:
```java
if (!java.security.MessageDigest.isEqual(
        expectedKey.getBytes(StandardCharsets.UTF_8),
        (providedKey != null ? providedKey : "").getBytes(StandardCharsets.UTF_8))) { ... }
```
Pair this with SEC-08's fail-closed fix — right now a misconfigured key check is both fail-open *and* comparison-weak.

#### SEC-NEW-04 — Health-check bypass in `InternalApiKeyFilter` uses `startsWith`, not exact match
**Severity:** Low | **Category:** Access Control | **File:** `watch-party/.../security/InternalApiKeyFilter.java:36-40`

```java
if (path.startsWith("/api/health")) { filterChain.doFilter(request, response); return; }
```
This also matches any future path like `/api/health-admin` or `/api/healthcheck-internal`, bypassing the API key filter for anything that happens to share the prefix. Not exploitable today (no such path exists), but it's a footgun for the next endpoint someone adds. Use `path.equals("/api/health") || path.startsWith("/api/health/")`.

#### SEC-NEW-05 — Unsanitized chat sender name rendered in watch-party UI
**Severity:** Medium | **Category:** XSS | **Files:** `frontend/src/hooks/useWatchParty.jsx:98`, `frontend/src/components/watch-party/LiveChat.jsx:88-93`

Chat `message` content is rendered as text (safe), but the `sender`/`userName` value arriving over the WebSocket is rendered without truncation or escaping validation on the client. Combined with SEC-NEW-02 (no server-side auth on the WebSocket, and a client can set an arbitrary `userName` in the join payload), this is the concrete XSS delivery mechanism the original audit's frontend section didn't connect: an unauthenticated attacker who can reach the WebSocket (which, per SEC-NEW-02, is trivial) can set their display name to attacker-controlled content and have it rendered to every party member. Combine the fix here (truncate/escape rendered `userName`) with SEC-NEW-02 (require auth to even get a `userName` in the first place) — either alone is a partial mitigation, both together close it.

#### SEC-NEW-06 — hate-api Dockerfile silently ignores its own pinned `requirements.txt` (production-blocking; caused a real incident during this engagement)
**Severity:** High | **Category:** Dependency Management / Supply Chain | **File:** `hate-api/Dockerfile:17-22` vs `hate-api/requirements.txt`

This is covered in full in §11.1 (DevOps Review) because it is fundamentally an infrastructure/build defect, but it belongs here too because its *effect* is a security-relevant one: **the deliberate, documented version pin exists specifically to prevent a known incompatibility**, and the Dockerfile bypasses it, silently pulling latest (unaudited, unpinned) versions of `torch`, `transformers`, and `fastapi` on every image rebuild. Unpinned production dependencies are a supply-chain risk independent of the specific bug they triggered here.

---

## 8. Performance Review

### PERF-01 — N+1 Redis Reads in `getPartyMembers()`

**Severity:** High | **Effort to fix:** Low

**Description.** `WatchPartyService.getPartyMembers()` iterates over all member keys in a Redis Set, then issues one individual `GET` call per member to fetch their `UserDataDTO`. This is an N+1 Redis round-trip pattern.

**Evidence.**
```java
// watch-party WatchPartyService.java lines 222-247
Set<Object> memberKeys = redisService.getSetMembers(membersKey);   // 1 call
for (Object memberKeyObj : memberKeys) {
    Long userId = extractUserIdFromMemberKey(...);
    String userDataKey = partyKey + ":user:" + userId;
    Object userData = redisService.getValue(userDataKey);           // 1 call per member
    ...
}
```
For a party of 20 members, this issues 21 sequential Redis round-trips.

**Impact.** At 1ms per Redis call, a 20-person party takes at least 20ms just for member fetching. Under load (many concurrent `getParty` calls), this creates significant Redis connection pressure.

**Recommended Fix.** Use Redis pipeline or `MGET`:
```java
// Collect all userDataKeys first, then fetch in one MGET
List<String> keys = memberKeys.stream()
    .map(k -> partyKey + ":user:" + extractUserId(k.toString()))
    .toList();
List<Object> values = redisTemplate.opsForValue().multiGet(keys);
```

---

### PERF-02 — HTTP Call Inside `@Transactional` Holds DB Connection Open

**Severity:** High | **Effort to fix:** Low

**Description.** `WatchPartyService.create()` and `join()` in the backend call the watch-party HTTP microservice **while inside a `@Transactional` block**. This holds the MySQL connection open for the full duration of the HTTP round-trip (up to the 10 s `readTimeout`).

**Evidence.**
```java
// backend WatchPartyService.java
@Transactional
public WatchParty create(WatchPartyUserDTO userDTO, Long movieId) {
    Movie movie = movieRepository.findById(movieId)...;   // acquires connection
    ...
    callMicroserviceInitialize(...);    // HTTP call — holds connection
    ...
    return watchPartyRepository.save(watchParty);
}
```

**Impact.** With a default connection pool of 10 connections and a 10 s HTTP timeout, only ~1 RPS of party creation can be sustained before the pool is exhausted. Other requests requiring a DB connection will queue behind them. *(See PERF-NEW-01 for the connection-pool sizing follow-up to this same defect.)*

**Recommended Fix.** Move all microservice calls **outside** the `@Transactional` boundary. Use a two-phase pattern:
1. Transaction 1: validate inputs and create the MySQL record with status `INITIALIZING`.
2. HTTP call (no transaction).
3. Transaction 2: update status to `ACTIVE` or delete the record on failure.

---

### PERF-03 — Caffeine Cache Has No Invalidation on Write

**Severity:** High | **Effort to fix:** Low

**Description.** The `exploreFeed` and `exploreForum` caches in `FeedService` are populated on first read and expire after 15 minutes. There is no cache eviction when new posts or forums are created, deleted, or updated.

**Evidence.**
```java
// FeedService.java
@Cacheable(value = "exploreFeed", key = "...")
public PostPageResponse getExploreFeed(int page, int size, String sortBy) { ... }
```
No `@CacheEvict` exists anywhere in the codebase:
```bash
$ grep -rn "@CacheEvict" backend/src/main/java/
# No results
```

**Impact.** A user who creates a post will not see it in the explore feed for up to 15 minutes. A deleted post (or hate-speech removal) will remain visible in the cached feed for up to 15 minutes.

**Recommended Fix.** Add `@CacheEvict(value = "exploreFeed", allEntries = true)` on `PostService.addPost()`, `deletePost()`, and `updatePost()`. Similarly for forums.

---

### PERF-04 — `HateSpeechScheduler` Loads All of Today's Comments Into Memory

**Severity:** High | **Effort to fix:** Medium

**Description.** The nightly hate-speech scanner calls `commentRepository.findAllByCreatedAtBetween(yesterday, now)` — this fetches all comment documents for the last 24 hours into Java heap memory as a `List<Comment>` before any processing begins.

**Evidence.**
```java
// HateSpeechScheduler.java lines 29-34
Instant yesterday = now.minus(1, ChronoUnit.DAYS);
List<Comment> todayComments = commentRepository.findAllByCreatedAtBetween(yesterday, now);
for (Comment comment : todayComments) {
    cleanComment(comment);
}
```

**Impact.** At 50,000 comments/day (medium scale), this loads ~50 MB or more of JSON into heap, potentially triggering GC pressure or OOM. The application configured with `-Xmx512m` (compose.yaml) makes this worse. *(See HS-06 for the full end-to-end quantitative breakdown of this same code path.)*

**Recommended Fix.** Use Spring Data's `Stream<Comment>` support with pagination:
```java
// Process in pages of 100 using a Pageable cursor
Page<Comment> page = commentRepository.findByCreatedAtBetween(yesterday, now, PageRequest.of(0, 100));
while (page.hasContent()) {
    page.getContent().forEach(this::cleanComment);
    page = commentRepository.findByCreatedAtBetween(yesterday, now, page.nextPageable());
}
```

---

### PERF-05 — `deleteKeysByPattern()` Uses Redis `KEYS` in Production

**Severity:** High | **Effort to fix:** Low

**Description.** `RedisService.deleteKeysByPattern()` calls `redisTemplate.keys(pattern)`, which executes Redis `KEYS` — a blocking O(N) command that scans the entire keyspace. Redis is single-threaded; this blocks all other commands.

**Evidence.**
```java
// RedisService.java lines 202-208
public Long deleteKeysByPattern(String pattern) {
    Set<String> keys = redisTemplate.keys(pattern);  // ← Redis KEYS command
    if (keys != null && !keys.isEmpty()) {
        return redisTemplate.delete(keys);
    }
    return 0L;
}
```
Similarly `getKeysByPattern()` on line 198.

**Impact.** With thousands of party keys, this command can block Redis for hundreds of milliseconds, causing timeouts across all Redis-dependent features (watch-party, cache). This is a well-known Redis anti-pattern for production use.

**Recommended Fix.** Use `SCAN` with a cursor instead of `KEYS`:
```java
ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
try (Cursor<String> cursor = redisTemplate.scan(options)) {
    cursor.forEachRemaining(redisTemplate::delete);
}
```

---

### PERF-06 — `AccessService.canDeleteComment()` Issues Up to 3 Sequential MongoDB Queries

**Severity:** Medium | **Effort to fix:** Medium

**Description.** `AccessService.canDeleteComment()` fetches the comment, then potentially the post, then potentially the forum — up to 3 sequential MongoDB round-trips in the hot path of every delete-comment request.

**Evidence.**
```java
// AccessService.java lines 73-110
Comment comment = mongoTemplate.findById(commentId, Comment.class);  // query 1
Post post = mongoTemplate.findById(comment.getPostId(), Post.class); // query 2
Forum forum = mongoTemplate.findById(post.getForumId(), Forum.class);// query 3
```

**Impact.** Each comment delete is 3 DB reads before the actual delete logic runs. Under high delete frequency, this creates unnecessary read load.

**Recommended Fix.** Embed `postOwnerId` and `forumId` directly in the `Comment` document (denormalized), eliminating the need to follow the comment → post → forum chain for access checks.

---

### PERF-07 — `getMoviesOverview()` Uses Unsafe Raw Cast

**Severity:** Medium | **Effort to fix:** Low

**Description.** `MovieService.getMoviesOverview()` calls a native query and casts the result as `Object[]` after casting from `Object`. This is type-unsafe and can produce `ClassCastException` at runtime.

**Evidence.**
```java
// MovieService.java lines 72-75
Object resultObj = movieRepository.getMovieCountAndTotalViews(orgId);
Object[] result = (Object[]) resultObj;  // ← unchecked cast, can throw CCE
int numberOfMovies = ((Number) result[0]).intValue();
int totalViews = ((Number) result[1]).intValue();
```

**Impact.** If the JPQL query returns an empty result or changes its projection, this crashes with `ClassCastException` — an unhandled 500.

**Recommended Fix.** Define a projection interface or use `@SqlResultSetMapping` with a proper DTO. Alternatively use Spring Data's `@Query` with a DTO constructor expression.

---

### PERF-08 — No Database Indexes on `ratingCount`, `averageRating` (Movie Sorting)

**Severity:** Medium | **Effort to fix:** Low

**Description.** `MovieRepository` supports filtering by genre and sorting by rating, but there are no database indexes defined on `average_rating`, `rating_count`, or `genre` on the `movies` table.

**Evidence.**
- `Movie.java` entity has no `@Index` annotations beyond the default primary key.
- `MovieSpecification.java` builds dynamic `WHERE` clauses on `genre`, `name`, and other fields.
- `User` has `@Index(name = "idx_user_email", columnList = "email")` — showing the team knows how to add indexes but forgot movies.

**Impact.** Full table scans on every movie search/filter query as the catalog grows.

**Recommended Fix.**
```java
@Table(name = "movies", indexes = {
    @Index(name = "idx_movie_genre", columnList = "genre"),
    @Index(name = "idx_movie_rating", columnList = "average_rating DESC"),
    @Index(name = "idx_movie_admin", columnList = "admin_id")
})
```

---

### PERF-09 — `SimpleClientHttpRequestFactory` Does Not Support Connection Pooling

**Severity:** Medium | **Effort to fix:** Low

**Description.** `AppConfig.restTemplate()` uses `SimpleClientHttpRequestFactory`, which creates a **new TCP connection per request** rather than using a connection pool.

**Evidence.**
```java
// AppConfig.java
SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
factory.setConnectTimeout(5000);
factory.setReadTimeout(10000);
return new RestTemplate(factory);
```

**Impact.** Every call to hate-api and watch-party microservice opens a new TCP connection (TLS handshake overhead). Under moderate load, TCP connection exhaustion can occur.

**Recommended Fix.** Use `HttpComponentsClientHttpRequestFactory` backed by Apache HttpClient with a pooling connection manager:
```java
PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
cm.setMaxTotal(50);
cm.setDefaultMaxPerRoute(20);
CloseableHttpClient client = HttpClients.custom().setConnectionManager(cm).build();
return new RestTemplate(new HttpComponentsClientHttpRequestFactory(client));
```

---

### New findings from the independent review

#### PERF-NEW-01 — No HikariCP tuning; Spring Boot defaults (10 connections) are undersized for this workload
**Severity:** Medium | **Category:** Database / Scalability

`application.properties` sets `spring.datasource.url/username/password` and nothing else for the connection pool — Spring Boot's HikariCP defaults apply (`maximum-pool-size=10`). Combined with PERF-02 (HTTP calls held open inside `@Transactional`, each holding a connection for up to the 10s `readTimeout`), the effective sustainable throughput for *any* endpoint that touches both MySQL and an outbound HTTP call is roughly one request per second before the pool is exhausted and new requests start queuing behind `connection-timeout` (default 30s). This is the same root cause as PERF-02 wearing a different hat — fixing PERF-02 (move HTTP calls out of the transaction) fixes most of the pressure, but the pool should still be sized deliberately rather than left at the framework default for a service that also talks to MongoDB and two other HTTP services.

**Recommended fix:**
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=10000
```
(Numbers are a starting point, not gospel — size against actual measured concurrency once PERF-02 is fixed.)

#### PERF-NEW-02 — Vote/comment read-modify-write pattern independently rediscovered with the identical atomic fix
This is the same underlying defect as REL-01 (§9). Both this independent review and the original audit arrived at the identical root cause and fix (`mongoTemplate.updateFirst(..., new Update().inc(...))`) with no cross-contamination between the two reviews. Treat this as strong corroboration rather than a separate new finding — see §15.1.

---

## 9. Reliability Review

### REL-01 — Race Condition on Vote and Comment Counters

**Severity:** Critical | **Effort to fix:** Medium

**Description.** Vote counts (`upvoteCount`, `downvoteCount`, `score`) and comment counts (`commentCount`) are read into memory, incremented in Java, and written back. In a multi-request environment, two concurrent votes on the same post will produce a lost update.

**Evidence.**
```java
// VoteService.java — incrementVote()
target.incrementUpvote();   // in-memory increment on the loaded object
target.updateScore();
postRepository.save(post);  // overwrites whatever was saved between the read and this write
```
```java
// CommentService.java — addComment()
post.setCommentCount(post.getCommentCount() + 1);  // read-modify-write
postRepository.save(post);
```

**Impact.** Under concurrent load, vote counts can be permanently under-counted. Example: two users upvote simultaneously, both read `upvoteCount = 5`, both save `6` — the actual count should be `7`. This is a classic lost-update problem in a document database with no optimistic locking.

**Recommended Fix.** Use MongoDB atomic operators instead of read-modify-write:
```java
// Use MongoTemplate with $inc operator
Update update = new Update().inc("upvoteCount", 1).inc("score", 1);
mongoTemplate.updateFirst(query, update, Post.class);
```
No entity load is needed; no race condition is possible.

---

### REL-02 — Watch-Party State Split Between MySQL and Redis With No Compensation

**Severity:** Critical | **Effort to fix:** Medium

**Description.** `WatchPartyService.create()` (backend) saves a `WatchParty` row to MySQL inside a `@Transactional` method, then calls the watch-party microservice to initialize Redis state. If the microservice call fails or times out, the MySQL row is committed (the transaction only covers MySQL) but Redis has no party — the party ID is permanently orphaned.

**Evidence.**
```java
// backend WatchPartyService.java lines 40-61
@Transactional                          // covers only MySQL
public WatchParty create(...) {
    ...
    callMicroserviceInitialize(...);    // can throw after MySQL commit
    WatchParty watchParty = ...;
    return watchPartyRepository.save(watchParty);  // saved BEFORE the call
```
The save is **after** the call, but the `@Transactional` spans both. The microservice call is **inside** the transaction, meaning the MySQL connection is held open during the HTTP call (up to 10 s timeout). If MySQL has a `wait_timeout`, this can exhaust the connection pool under load.

The reverse is also a problem in `delete()`: microservice is called first (Redis cleared), then MySQL is updated. If the MySQL update fails, Redis is empty but MySQL still shows `ACTIVE`.

**Impact.** Parties stuck in `ACTIVE` state in MySQL with no corresponding Redis state → all subsequent reads return 500. Connection pool exhaustion under moderate load (connections held open during 10 s HTTP calls).

**Recommended Fix.**
1. **Immediate:** Move `callMicroserviceInitialize()` to **after** `watchPartyRepository.save()` and outside the `@Transactional` boundary. Add a compensating delete if the call fails.
2. **Structural:** Introduce a party `state` field (`INITIALIZING` → `ACTIVE`) to handle partial creation gracefully. A background reconciler can clean up `INITIALIZING` parties older than 30 seconds.

---

### REL-03 — `addComment()` Fetches Post Twice Without Null Check on First Fetch

**Severity:** High | **Effort to fix:** Low

**Description.** `CommentService.addComment()` validates the post via `canComment(postId)` (one `mongoTemplate.findById` call), then fetches it again on line 45 — this time without a null check.

**Evidence.**
```java
// CommentService.java lines 33-48
canComment(postId);   // fetches post, throws if null/deleted ✓
...
Post post = mongoTemplate.findById(postId, Post.class);  // fetches AGAIN
post.setCommentCount(post.getCommentCount() + 1);        // NPE if deleted between calls
```

**Impact.** If the post is soft-deleted between the two fetches (e.g., concurrent delete), `post` is `null` and `post.setCommentCount()` throws a `NullPointerException` — an unhandled 500 error.

**Recommended Fix.** Return the `Post` from `canComment()` and reuse it, eliminating the second fetch.

---

### REL-04 — Verification Code Is Not Deleted After Failed Password Reset

**Severity:** High | **Effort to fix:** Low

**Description.** `VerificationService.updatePasswordByVerificationCode()` calls `verify()` to check the code, updates the password if it matches, but **never deletes the verification record**. The code remains valid indefinitely (until the nightly cleanup runs after 10 minutes).

**Evidence.**
```java
// VerificationService.java lines 103-115
public void updatePasswordByVerificationCode(...) {
    Optional<Verfication> verification = verify(email, verificationCode);
    if (verification.isPresent()) {
        switch (role) {
            case "ROLE_USER" -> updateUserPassword(email, password);
            ...
        }
        // ← no verificationRepository.delete() here
    }
}
```
Compare with `verifyEmail()` (line 220): `verificationRepository.delete(verification.get())` — present there but missing in `updatePasswordByVerificationCode`.

**Impact.** The same one-time password-reset code can be reused multiple times within the 10-minute window. If an attacker observes the code (e.g., email interception), they can reset the password again after the victim has already used it.

**Recommended Fix.** Add `verificationRepository.delete(verification.get())` immediately after confirming the code is valid, before updating the password.

---

### REL-05 — `forgetPassword()` Returns an Empty `Verfication` on Email Failure

**Severity:** Medium | **Effort to fix:** Low

**Description.** `VerificationService.forgetPassword()` returns `new Verfication()` (an empty object with no ID) when the email send fails, instead of throwing an exception.

**Evidence.**
```java
// VerificationService.java lines 68-76
public Verfication forgetPassword(String email, String role) {
    if (sendVerificationEmail(email, code)) {
        return addVerfication(email, code, role);
    } else {
        return new Verfication();  // ← silent failure with garbage object
    }
}
```

**Impact.** The caller receives a non-null `Verfication` with a null ID and null code. Any subsequent null check on `getCode()` or `getId()` may not catch this. The user believes a reset email was sent when it was not.

**Recommended Fix.** Throw `RuntimeException("Failed to send password reset email")` consistent with how `signUp()` handles it.

---

### REL-06 — Participant Count Decrement Is Not Atomic in Watch-Party Service

**Severity:** High | **Effort to fix:** Low

**Description.** `WatchPartyService.decrementParticipantCount()` and `incrementParticipantCount()` perform a Redis read-modify-write: fetch the current count as a string, parse it, add/subtract 1, and write back. This is not atomic.

**Evidence.**
```java
// watch-party WatchPartyService.java lines 262-274
private void incrementParticipantCount(String partyKey) {
    Object currentValue = redisService.getHashValue(partyKey, "currentParticipants");
    int current = Integer.parseInt(currentValue.toString());
    redisService.setHashValue(partyKey, "currentParticipants", String.valueOf(current + 1));
}
```

**Impact.** Concurrent join/leave operations produce incorrect participant counts (same lost-update problem as REL-01 but in Redis). `RedisService` already has `incrementHashValue()` and `decrementHashValue()` methods that use Redis `HINCRBY` (atomic) — they are just not being used. *(See REL-NEW-01 for the fully-traced race window this produces.)*

**Recommended Fix.**
```java
private void incrementParticipantCount(String partyKey) {
    redisService.incrementHashValue(partyKey, "currentParticipants");
}
private void decrementParticipantCount(String partyKey) {
    return redisService.decrementHashValue(partyKey, "currentParticipants").intValue();
}
```

---

### REL-07 — `cascadeDeleteCommentAsync` Updates `commentCount` With Wrong Subtraction Value

**Severity:** High | **Effort to fix:** Medium

**Description.** When a comment is deleted, `CascadeDeletionService.cascadeDeleteCommentAsync()` subtracts the count of **all deleted comments** (including all descendants) from the post's `commentCount`, then also subtracts that same count from the **parent comment's** `numberOfReplies`. This double-counts descendants in the parent's reply count.

**Evidence.**
```java
// CascadeDeletionService.java lines 409-421
int totalComments = softDeleteCommentsByIdsBatch(allIds, deletedAt);

post.setCommentCount(post.getCommentCount() - totalComments);  // correct: all descendants
...
parent.setNumberOfReplies(parent.getNumberOfReplies() - totalComments);  // wrong: should be 1
```
If a user deletes a top-level comment with 5 replies: `totalComments = 6`. The parent's `numberOfReplies` is decremented by 6, but the parent only lost 1 direct child.

**Impact.** `numberOfReplies` becomes negative for the parent comment, corrupting UI display. The corruption is permanent (no recalculation mechanism).

**Recommended Fix.** The parent comment's `numberOfReplies` should be decremented by `1` (direct child removed), not by `totalComments` (entire subtree). Only the `commentCount` on the post should use `totalComments`.

---

### REL-08 — WebSocket Control Channel Has No Authentication

**Severity:** Critical | **Effort to fix:** Medium

**Description.** The WebSocket STOMP endpoint `/ws` in the watch-party service is configured with `.setAllowedOriginPatterns("*")` and has no authentication. Any client that knows a `partyId` can send `PLAY`, `PAUSE`, `SEEK` events to all party members.

**Evidence.**
```java
// WebSocketConfig.java line 24
.setAllowedOriginPatterns("*")

// WebSocketController.java line 22
@MessageMapping("/party/{partyId}/control")
public void handleControl(@DestinationVariable String partyId, @Payload PartyEvent event) {
    // no authentication check — accepts any message
    redisPublisher.publish(partyId, event);
}
```

**Impact.** Any anonymous client can disrupt any watch party by sending pause/seek events. `partyId` is a UUID so guessing it is hard **in the default case**, but see SEC-NEW-02 for why that assumption doesn't hold once a client can *choose* its own `partyId`. Chat messages can be sent by unauthenticated users with any `userName` — see SEC-NEW-05 for the resulting XSS delivery path.

**Recommended Fix.**
1. Validate a JWT in the STOMP `CONNECT` frame using a `ChannelInterceptor`.
2. Extract `userId` from the JWT and compare it against the party's member set before broadcasting.
3. Restrict `setAllowedOriginPatterns` to the frontend's known origin.

---

### REL-09 — `addUser()` Stores Plain-Text Password

**Severity:** Critical | **Effort to fix:** Low

**Description.** `UserService.addUser()` saves the user with the password as received — no hashing. The hashing happens in `VerificationService.addVerfication()` (which stores a hashed password in the verification table), but when `verifyEmail()` calls `userService.addUser(email, password)`, the `password` argument is the **already-hashed** value from the verification record. So the password that reaches `addUser` is pre-hashed, but `addUser` itself does not hash it — meaning if `addUser` is ever called from another path with a plain-text password, it will be stored in plain text.

**Evidence.**
```java
// UserService.java lines 42-48
public User addUser(String email, String password) {
    User user = User.builder()
            .email(email)
            .password(password)   // ← no BCrypt encoding here
            .build();
    return userRepository.save(user);
}
```

**Impact.** If this method is ever refactored or called from a new code path with a plain-text password, users' passwords are stored without hashing — a catastrophic security failure. The current flow happens to be safe (password is pre-hashed), but the method's contract is broken: callers must know to pre-hash. This is a latent bug.

**Recommended Fix.** Either hash inside `addUser()` (and remove the pre-hashing in `VerificationService`), or rename the method to `addUserWithPrehashedPassword()` and add a doc comment. The former is cleaner and safer.

---

### New findings from the independent review

#### REL-NEW-01 — Precise race-window trace for watch-party join/leave (extends REL-06 with root cause, not just symptom)
**Severity:** High | **Category:** Concurrency | **File:** `watch-party/.../WatchPartyService.java` (join ~85-119, leave ~124-158)

REL-06 correctly flags the participant-count increment/decrement as non-atomic. Independent review traced the *full* race, which is worse than a lost counter update:

```
Thread A: isMemberOfSet(user:123) → false
Thread B: isMemberOfSet(user:123) → false     (both pass the duplicate-join check)
Thread A: addToSet(user:123)                   (Redis SET dedupes — fine)
Thread B: addToSet(user:123)                   (no-op, already present)
Thread A: incrementParticipantCount()  → 2
Thread B: incrementParticipantCount()  → 3      (!)
```
Result: **one** actual member, but the party reports **3** participants. This is materially different from a simple undercount — it can *overcount*, meaning a party that should auto-delete when it reaches zero participants (per the existing `leaveParty` logic) may never do so, leaking Redis keys indefinitely (compounding the memory-growth risk already implicit in REL-06).

**Recommended fix:** wrap the whole "check membership → add → increment" sequence in a single Redis Lua script (`EVAL`) so it executes atomically, rather than trying to make each step individually atomic — the bug is in the *sequence*, not any one operation:
```lua
if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
  return 0
else
  redis.call('SADD', KEYS[1], ARGV[1])
  redis.call('HINCRBY', KEYS[2], 'currentParticipants', 1)
  return 1
end
```

#### REL-NEW-02 — No graceful shutdown for `watch-party`
**Severity:** Low | **Category:** Operational Resilience

No `@PreDestroy`/`server.shutdown=graceful` configuration exists. On `SIGTERM` (container stop, rolling deploy), active WebSocket sessions are dropped without notice and any Redis state mid-write is left as-is. Given `restart: unless-stopped` and the local-dev-oriented nature of the current deployment this is Low, not High, severity today — but it becomes relevant the moment this service is ever deployed behind an orchestrator that does rolling restarts. Add `server.shutdown=graceful` and `spring.lifecycle.timeout-per-shutdown-phase=20s` as a cheap first step.

---

## 10. Database Review

*(New top-level section. ARC-01 in §4 (no cross-store transactions), ARC-07 (ID conversion hack), and PERF-08 (missing movie indexes) cover the most important architectural database issues already in the original audit. Independent schema-level review adds the following, with severities recalibrated against actual reachability — see the note on DB-NEW-01, the clearest example in this document of the kind of verification this whole review tries to model.)*

#### DB-NEW-01 — `User` entity cascades `CascadeType.ALL` + `orphanRemoval=true` to six child collections — a dormant landmine, not (yet) an active bug
**Severity:** Medium *(independently downgraded from an initial Critical rating — see below)* | **Category:** Data Integrity | **File:** `backend/src/main/java/org/example/backend/user/User.java:110-132`

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private List<WatchHistory> watchHistory = new ArrayList<>();
// ... same pattern for WatchLater, LikedMovie, Follows (both directions), MovieReview
```

If a `User` entity is ever hard-deleted (`userRepository.delete(...)`/`.deleteById(...)`), Hibernate will cascade-delete all six related collections, **bypassing the `isDeleted` soft-delete flag those same entities define**. Verified this is not currently reachable: a full-repository search for `userRepository.delete` / `.deleteById(` found zero call sites — the codebase only ever soft-deletes users. So this is not an active bug today.

It is, however, a real design defect that will detonate the moment anyone adds the single most predictable feature request for a social platform: a GDPR-style "delete my account" hard-delete path, or an admin cleanup script. Whoever adds that feature will reasonably expect `userRepository.delete()` to do what it says, and will get a silent, unaudited hard cascade instead of the soft-delete-and-cleanup semantics the rest of the codebase uses everywhere else. **Recommended fix:** remove `CascadeType.ALL`/`orphanRemoval` now, before that feature exists, and route all deletion through an explicit `UserDeletionService` that mirrors the soft-delete pattern already used for `CascadeDeletionService`'s MongoDB side.

#### DB-NEW-02 — `Vote.isPost: Boolean` should be an enum; compound index references a field name (`targetType`) that doesn't exist
**Severity:** Medium | **Category:** Schema Design | **File:** `backend/src/main/java/org/example/backend/vote/Vote.java:22,33-34`

```java
@CompoundIndex(name = "target_type", def = "{'targetType': 1, 'targetId': 1, 'voteType': 1}")
// ...
private Boolean isPost;   // <-- the actual field is isPost, not targetType
```
The compound index definition references a MongoDB field `targetType` that does not exist anywhere in the document — the actual discriminator field is `isPost`. This index either silently fails to provide the intended query coverage or was written against a since-renamed field and never updated. Either way, queries that assume this index is backing them are doing a collection scan. Beyond the index bug: a `Boolean` discriminator for "is this a vote on a post or a comment" doesn't extend (what happens when forum-level voting is added?) and reads worse at every call site than `VoteTargetType.POST`/`.COMMENT` would. **Recommended fix:** convert to an enum, and regenerate the compound index against the corrected field name — verify via `db.votes.getIndexes()` in a real MongoDB instance that the new index is actually used (`explain()`) before considering this closed.

#### DB-NEW-03 — Inconsistent soft-delete: some MongoDB collections have no `deletedAt`, and at least one repository method hard-deletes despite the collection having an `isDeleted` field
**Severity:** Medium | **Category:** Data Integrity / Auditability

Most soft-deletable documents (`Post`, `Comment`) have `isDeleted` but not all have a `deletedAt` timestamp, meaning "when was this deleted" is unanswerable for compliance/audit purposes on some collections while answerable on others. Separately, `FollowingRepository` exposes a `deleteByUserIdAndForumId()` method that performs a genuine hard delete on a collection whose sibling entity elsewhere in the same domain uses soft-delete — an easy trap for the next engineer who assumes "this codebase soft-deletes" is a universal rule (it's the dominant pattern, but not a guaranteed one). **Recommended fix:** introduce a `@MappedSuperclass`/base document with `isDeleted` + `deletedAt` applied uniformly, and audit every repository for hard-delete methods that should be soft-delete instead.

#### DB-NEW-04 — No connection pool tuning (see PERF-NEW-01); `DataInitializer` is not idempotent
**Severity:** Low | **Category:** Data Initialization | **File:** `backend/src/main/java/org/example/backend/dataInitializer/DataInitializer.java`

If `app.data-init.enabled=true` and the app restarts (which, per §11.6, currently can't happen via the documented flow, but would if that gap is fixed), `DataInitializer` re-runs its seed logic with no existence check first, and will throw a unique-constraint violation on `testUser1@example.com` etc. on the second run. Cheap fix once §11.6 makes this path reachable at all: `if (userRepository.count() > 0) return;` guard at the top of `run()`.

---

## 11. DevOps & Infrastructure Review

*(The original audit's coverage here was limited to ARC-08/TEST-05 — observability only. This section is substantially expanded with findings that mostly required actually running the stack, not just reading it.)*

### 11.1 hate-api Dockerfile ignores its own pinned `requirements.txt` — verified root cause of a real incident

**Severity:** High | **Category:** Build/Dependency Management | **Files:** `hate-api/Dockerfile:17-22`, `hate-api/requirements.txt`

This was **discovered by actually running the stack**, not by reading code — the kind of defect a purely static audit structurally cannot find. `hate-api/requirements.txt` exists, is version-pinned, and carries an explicit comment explaining why:

```
# Pinned versions — do not upgrade without testing the full Docker build.
# Root cause: transformers>=4.52 requires torch>=2.7 (for float8_e8m0fnu dtype),
# but the CPU torch whl only provides torch 2.5.x. Pin both together.
nltk==3.9.1
fastapi==0.115.12
uvicorn==0.34.2
torch==2.5.1
transformers==4.47.1
```

But the Dockerfile **never runs `pip install -r requirements.txt`**:

```dockerfile
COPY requirements.txt .
RUN pip install --no-cache-dir \
    torch --index-url https://download.pytorch.org/whl/cpu && \
    pip install --no-cache-dir \
    nltk fastapi uvicorn transformers      # <-- unpinned, pulls latest
```

`requirements.txt` is copied into the image and then completely ignored. During a live rebuild of this project, `docker compose build` pulled **`transformers==5.12.1`** and **`huggingface-hub==1.21.0`** — versions far past the documented-safe pin — and the container **crash-looped at startup**: `huggingface_hub.errors.LocalEntryNotFoundError: Cannot find the requested files in the disk cache and outgoing traffic has been disabled`. Root cause: newer `huggingface_hub`/`transformers` no longer honor the `TRANSFORMERS_CACHE` env var (only `HF_HUB_CACHE`), so the model that was correctly cached to `/models` at build time couldn't be found at runtime, and `TRANSFORMERS_OFFLINE=1` correctly refused to fall back to the network — a secondary, correct safety behavior masking the real bug.

This was fixed live (adding `HF_HUB_CACHE=/models` and `HF_HUB_OFFLINE=1` to the runtime stage `ENV`), but **the underlying defect — the Dockerfile ignoring `requirements.txt` — is still present** and will reintroduce a version-drift bug (this one or a new one) on the next unrelated image rebuild, since nothing pins the actual installed versions.

**Recommended fix:**
```dockerfile
COPY requirements.txt .
RUN pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu \
    torch==2.5.1 && \
    pip install --no-cache-dir -r requirements.txt
```
And add a CI step (§11.3) that fails the build if `pip list --format=freeze` inside the built image doesn't match `requirements.txt`.

### 11.2 `JPA_DDL_AUTO` disagrees between dev and prod env templates, and neither uses a migration tool
**Severity:** Medium | **Category:** Schema Evolution | **Files:** `.env:20` (`validate`), `backend/.env.prod:17` (`update`)

Verified directly — the two committed templates literally disagree:
```
.env:20:              JPA_DDL_AUTO=validate
backend/.env.prod:17: JPA_DDL_AUTO=update
```
`update` in what's meant to be the production-flavored template is the more dangerous of the two (silent, untracked, unreviewable schema alterations applied by Hibernate at startup — including potentially dropping columns removed from an entity). There is no Flyway/Liquibase anywhere in the project, so `ddl-auto` is the *only* schema evolution mechanism that exists, which makes getting this setting right unusually important. **Recommended fix:** set `validate` everywhere, and introduce Flyway (`flyway-core` + `spring-boot-starter` autoconfiguration is close to zero setup cost for a project this size) so schema changes become versioned, reviewable SQL files instead of implicit Hibernate inference.

### 11.3 CI covers exactly one of four services
**Severity:** Medium | **Category:** CI/CD

`.github/workflows/backend-tests.yml` is the **only** workflow in the repository (verified: `.github/workflows` contains one file, no `dependabot.yml`, no other workflow). `watch-party`, `hate-api`, and `frontend` have zero CI — no build verification, no test execution (frontend has no tests to run in any case, per FE-08), no lint step, no Docker image build check. A change that breaks the `watch-party` build or introduces a `hate-api` regression (like §11.1) will not be caught until someone runs `docker compose up` locally.

**Recommended fix (quick win):** add `watch-party-tests.yml` (mirror the backend workflow), `hate-api-lint.yml` (at minimum `pip install -r requirements.txt && python -c "import app"` as a smoke import test, which alone would have caught §11.1 immediately), and `frontend-lint.yml` (`npm ci && npm run lint`, plus `npm run build` as a build-integrity check even before tests exist). Add Dependabot for at least the three `pom.xml`/`package.json`/`requirements.txt` ecosystems — zero-config, catches exactly the kind of drift in §11.1.

### 11.4 No resource governance for the databases; only `hate-api` has container resource limits
**Severity:** Low | **Category:** Infrastructure

`compose.yaml` sets `deploy.resources.limits` (2 CPU / 2G memory) only for `hate-api`. `mysql`, `mongodb`, and `redis` have no CPU/memory ceiling at all — a runaway query, an unbounded aggregation, or the cache-growth issue implicit in REL-06/REL-NEW-01 could consume unbounded host memory with nothing to stop it. `backend`/`watch-party` set JVM heap flags (`-Xmx512m`) but no *container-level* memory limit, so unexpected off-heap growth (direct buffers, native library memory) has no ceiling either. This is a "fine until it isn't" issue: harmless on a dev laptop, a real incident risk the first time this runs on shared infrastructure. Add `deploy.resources.limits` to every service.

### 11.5 No backup/restore strategy for either database
**Severity:** Medium | **Category:** Reliability

`mysql-data` and `mongo-data` are plain named Docker volumes with no backup automation, no documented restore procedure, and no snapshot schedule anywhere in the repo or docs. For a project that stores user accounts, posts, and reviews, this means **any data-loss event (bad migration, disk failure, accidental `docker volume rm`) is unrecoverable**. This doesn't need to be sophisticated — even a documented `mysqldump`/`mongodump` cron container added to `compose.yaml` writing to a bind-mounted host directory would close the most acute version of this gap.

### 11.6 `DataInitializer` can never run via the documented setup path
**Severity:** Low | **Category:** Developer Experience | **Files:** `DataInitializer.java:41` (`@Profile("dev")`), `.env:64`/`compose.yaml:163`/`backend/.env.prod:59` (all `SPRING_PROFILES_ACTIVE=prod`)

`DataInitializer` (which seeds test users/orgs/forums/posts for local development) is gated to `@Profile("dev")`. But every environment file involved in the officially documented `docker compose up --build` flow — root `.env`, `compose.yaml`'s explicit `environment:` override, and `backend/.env.prod` — hardcodes `SPRING_PROFILES_ACTIVE=prod`. This means **a new engineer following the README's documented setup path will never see seed data**; the profile that would trigger it is unreachable without manually editing an env file the setup instructions don't mention touching. Either wire a `SPRING_PROFILES_ACTIVE=dev` override into a docker-compose *dev* override file (`compose.override.yaml`, the idiomatic Compose pattern for this exact problem), or gate `DataInitializer` on its own dedicated `app.data-init.enabled` flag instead of the profile (the flag already exists — it's just redundant with, and shadowed by, the profile gate).

---

## 12. Frontend Review

### FE-01 — JWT Stored in `sessionStorage` (XSS Accessible)

**Severity:** High | **Effort to fix:** Medium

**Description.** The JWT token is stored in `sessionStorage` under the key `CINEMATE_JWT_TOKEN`. Any JavaScript executing on the page (including injected scripts from XSS) can read it with `sessionStorage.getItem("CINEMATE_JWT_TOKEN")`.

**Evidence.**
```javascript
// sign-in-api.jsx line 16
sessionStorage.setItem(JWT.STORAGE_NAME, token);

// api-client.jsx line 12
const token = sessionStorage.getItem(JWT.STORAGE_NAME);
```
`sessionStorage` is accessible to all scripts running in the same origin — identical to `localStorage` in terms of XSS exposure.

**Impact.** A single XSS vulnerability anywhere in the application leads to full account takeover (token exfiltration). The token has a 24-hour lifetime, maximising the exfiltration window.

**Recommended Fix.** Use `HttpOnly` cookies to store the JWT. The browser automatically includes them in requests but prevents JavaScript access. This requires backend changes (set `Set-Cookie: jwt=...; HttpOnly; Secure; SameSite=Strict`) and removing the manual `Authorization` header injection from the Axios interceptor.

---

### FE-02 — Production Environment File Has `localhost` URLs Baked In

**Severity:** Critical | **Effort to fix:** Low

**Description.** `.env.production` contains `localhost` URLs. When the Docker image is built with these values, **all production API calls will attempt to hit `localhost:8080` from the user's browser**, which is not the production backend.

**Evidence.**
```env
# frontend/.env.production
VITE_API_BASE_URL=http://localhost:8080
VITE_API_WATCH_PARTY_BASE_URL=http://localhost:8081
```
Since Vite bakes environment variables at build time (`import.meta.env.*`), these values are compiled into the static JS bundle. Any Docker image built from this file is broken for any deployment that isn't local.

**Impact.** The production Docker image is non-functional. All API calls fail because no user's browser has a backend on `localhost:8080`. This likely means no real production deployment has worked without manual intervention. This is the finding where "Critical" means "the application, as configured, cannot be deployed at all" — worth calling out plainly since it's easy for severities to blur together in a 91-item list.

**Recommended Fix.** The `.env.production` file should contain real production URLs (or placeholders that are replaced in CI/CD). Use a runtime config injection approach (a served `config.js` file that sets `window.__CINEMATE_CONFIG__`) to avoid baking URLs into the bundle, allowing the same image to work across environments.

---

### FE-03 — Frontend Directly Connects to Watch-Party WebSocket (`localhost:8081`)

**Severity:** High | **Effort to fix:** Medium

**Description.** The frontend connects to the watch-party WebSocket service directly at `VITE_API_WATCH_PARTY_BASE_URL` (defaulting to `localhost:8081`). In the Docker Compose setup, the watch-party service is internal-only (`expose`, not `ports`), so the WebSocket connection from the browser **cannot reach it** in the containerized environment.

**Evidence.**
```javascript
// constants.jsx line 3
WATCH_PARTY_BASE_URL: import.meta.env.VITE_API_WATCH_PARTY_BASE_URL || "http://localhost:8081",
```
```yaml
# compose.yaml lines 160-161 — watch-party is internal only
expose:
  - "8081"
```
The WebSocket must go through the backend or an API gateway — it cannot be a direct browser → internal Docker service connection.

**Impact.** Watch-party WebSocket functionality is completely broken in the containerized deployment as described. The frontend architecture assumes watch-party is publicly reachable, which contradicts the backend's security model.

**Recommended Fix.** Proxy WebSocket connections through nginx:
```nginx
location /ws { proxy_pass http://watch-party:8081; proxy_http_version 1.1; proxy_set_header Upgrade $http_upgrade; proxy_set_header Connection "upgrade"; }
```
Update frontend to connect to the same origin for WebSocket (`/ws`).

---

### FE-04 — API Layer Files Are `.jsx` but Contain No JSX

**Severity:** Low | **Effort to fix:** Low

**Description.** All 20+ files in `frontend/src/api/` use the `.jsx` extension but contain only plain JavaScript (no JSX/React elements).

**Evidence.**
```
frontend/src/api/api-client.jsx      ← plain JS, no JSX
frontend/src/api/sign-in-api.jsx     ← plain JS, no JSX
frontend/src/api/watch-together-api.jsx  ← plain JS, no JSX
```

**Impact.** Misleading extension causes Vite/ESLint to apply JSX transform rules unnecessarily. IDEs may provide incorrect type hints and syntax highlighting. Minor but signals inconsistent conventions.

**Recommended Fix.** Rename to `.js`.

---

### FE-05 — Duplicate API Files (`forum-api.jsx` and `forums-api.jsx`, `post-api.jsx` and `posts-api.jsx`)

**Severity:** Medium | **Effort to fix:** Low

**Description.** There are two API files for forums (`forum-api.jsx` and `forums-api.jsx`) and two for posts (`post-api.jsx` and `posts-api.jsx`). This indicates fragmented, duplicated API layer modules.

**Evidence.**
```
frontend/src/api/forum-api.jsx
frontend/src/api/forums-api.jsx
frontend/src/api/post-api.jsx
frontend/src/api/posts-api.jsx
```

**Impact.** Different components import from different files, making it unclear which is canonical. Updates to API endpoints must be made in multiple places.

**Recommended Fix.** Consolidate into a single `forum-api.js` and `post-api.js`. Review imports across the project to update references.

---

### FE-06 — No Content Security Policy (CSP) Header

**Severity:** Medium | **Effort to fix:** Low

**Description.** The nginx configuration adds `X-Frame-Options`, `X-Content-Type-Options`, and `X-XSS-Protection` but no `Content-Security-Policy` header. The deprecated `X-XSS-Protection` header is also present (modern browsers ignore it).

**Evidence.**
```nginx
# nginx.conf lines 13-15
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;   # deprecated
# ← no Content-Security-Policy
```

**Impact.** Without CSP, inline scripts and third-party scripts can execute without restriction, increasing XSS impact (see FE-01, SEC-NEW-05 for concrete XSS vectors this would help contain). The `X-XSS-Protection` header has been removed from modern browsers (Chrome 78+) — it provides false security assurance.

**Recommended Fix.**
```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; connect-src 'self' ws://your-domain; img-src 'self' data:; style-src 'self' 'unsafe-inline';";
# Remove X-XSS-Protection
```

---

### FE-07 — No Error Boundaries in the React App

**Severity:** Medium | **Effort to fix:** Low

**Description.** No React error boundaries are visible in the component tree. An uncaught rendering error in any component will crash the entire page to a blank white screen.

**Evidence.** No `ErrorBoundary` component found in the source listing. `App.jsx` uses React Router with lazy-loaded routes and `Suspense` (`LoadingFallback` component exists), but no error boundary wraps route groups.

**Impact.** A rendering bug in one section (e.g., watch-party page) crashes the entire SPA. Users see a blank page with no explanation or recovery path.

**Recommended Fix.** Wrap major route groups with `ErrorBoundary` components that show a user-friendly error message and a "go back" button:
```javascript
class ErrorBoundary extends React.Component {
    constructor(props) { super(props); this.state = { hasError: false }; }
    static getDerivedStateFromError() { return { hasError: true }; }
    componentDidCatch(error, errorInfo) { console.error(error, errorInfo); }
    render() {
        if (this.state.hasError) {
            return <div>Something went wrong. <button onClick={() => window.location.reload()}>Reload</button></div>;
        }
        return this.props.children;
    }
}
```

---

### FE-08 — No Frontend Tests

**Severity:** High | **Effort to fix:** High

**Description.** The frontend has no test files — no unit tests, no component tests, no end-to-end tests.

**Evidence.** `package.json` has no test script, no vitest/jest dependency. No `__tests__` directory and no `.test.jsx` files anywhere in `frontend/src`.

**Impact.** UI regressions in the auth flow, vote submission, or watch-party joining go undetected until a user reports them. Refactoring any component carries high risk.

**Recommended Fix.**
1. Add Vitest + React Testing Library for unit/component tests.
2. Add at minimum: auth flow tests, API client interceptor tests, and critical component render tests.
3. Consider Playwright for end-to-end tests of the login → browse → watch-party flow.

---

### FE-09 — `joinRoomApi` Discards All Response Data Except `id`

**Severity:** Medium | **Effort to fix:** Low

**Description.** `joinRoomApi` returns only `{ id: response.data.id }`, but the backend actually returns a full `WatchPartyDetailsResponse` with `members`, `movieUrl`, `hostId`, etc. The frontend discards this data and must make a separate `getRoomApi` call to get party details.

**Evidence.**
```javascript
// watch-together-api.jsx lines 34-42
export async function joinRoomApi({ partyId }) {
    const response = await api.post(`/watch-party/join/${partyId}`);
    return {success: true, data: {id: response.data.id}}  // discards everything else
}
```
The comment block above (lines 21-31) even documents that the join response includes `members`, `movieUrl`, etc. — confirming the developer knew about the fields but didn't use them.

**Impact.** Unnecessary second API call on join (the `getRoomApi` called immediately after). Increased latency for joining a party.

**Recommended Fix.** Return the full `response.data` from `joinRoomApi` and eliminate the subsequent `getRoomApi` call.

---

### New findings from the independent review

#### FE-NEW-01 — Chat sender name not sanitized before render
Covered in §7 as SEC-NEW-05 (it's a security finding as much as a frontend one) — cross-referenced here for completeness of the frontend catalogue.

#### FE-NEW-02 — `PostComments.jsx` is 765 lines doing five unrelated jobs
**Severity:** Low | **Category:** Maintainability

One component handles user-name-cache fetching, comment CRUD, nested reply rendering, vote handling, and menu state — roughly 5-10x the size of a well-scoped component. This isn't a style nitpick; a component this size is the one that will accumulate the next ten bug reports because no single change to it can be reasoned about in isolation. Recommended: extract `useUserNameCache`, `useCommentVoting`, and a `CommentItem` component per the existing `*View`/hook conventions already used elsewhere in the codebase.

#### FE-NEW-03 — `console.log` of API response data across ~17 files
**Severity:** Low | **Category:** Hygiene / Minor Data Exposure

`forum-api.jsx`, `Browse.jsx`, `PostComments.jsx` and others log full API responses to the browser console in production builds (no `NODE_ENV` gating). Low severity (visible only to someone with devtools open on their own session) but real: it means response payloads — potentially including other users' profile data returned in list responses — sit in the browser console history. Gate behind a `log()` helper that no-ops outside development.

#### FE-NEW-04 — Search input fires an API call on every keystroke
**Severity:** Low | **Category:** Performance | **File:** `frontend/src/components/NavBar.jsx:28-30`

```javascript
useEffect(() => { handleSearch(); }, [searchValue]);
```
No debounce. A 10-character search term fires 10 sequential requests. Cheap fix, real (if minor) backend load reduction: standard 300ms `setTimeout`/debounce pattern.

---

## 13. Hate-Speech Moderation Service — Deep Review

**Scope:** `hate-api/app.py`, `HateSpeechService.java`, `HateSpeechScheduler.java`, and every call site: `PostService`, `ForumService`, `CommentService`.

### Architecture Overview (Current State)

```
User submits a post/comment/forum
     │
     ▼
PostService.addPost()  ─── @Transactional (MySQL conn held open) ───►
     │
     │  HTTP POST (synchronous, blocking, up to 10s timeout)
     ▼
hate-api:8000/api/hate/v1/analyze    (FastAPI + RoBERTa, 1 uvicorn worker)
     │
     │  NLTK sentence tokenize → model inference per sentence
     ▼
return Boolean
     │
     ▼ (if true)
PostService continues → MongoDB write → response
```

**Key characteristics:** synchronous, inline, blocking call on every write operation; up to 2 HTTP calls per operation (title + content checked separately); single uvicorn worker means the model can only process **1 request at a time** globally; the call happens **inside** `@Transactional`, holding a MySQL connection open; the nightly scheduler replays the same blocking HTTP path for **all** comments created in the last 24 hours.

---

### HS-01 — Synchronous Moderation Blocks Every Write Operation (Core Architecture Flaw)

**Severity:** Critical | **Effort to fix:** Medium

**Description.** Every user-facing write that creates content (post, comment, forum) blocks synchronously on an HTTP call to the hate-api before persisting anything. The hate-api runs a 130M-parameter transformer model (RoBERTa) on CPU. CPU inference time for a medium-length post is typically **200ms–2000ms** depending on text length and server load.

**Evidence — HTTP call inside the request path.**
```java
// PostService.java line 52
if (!hateSpeechService.analyzeText(addPostDto.getContent())
 || !hateSpeechService.analyzeText(addPostDto.getTitle())) {
    throw new HateSpeechException("hate speech detected");
}
```
Two sequential HTTP calls (short-circuit only helps when content is hateful). For innocent posts, **both** calls always execute.
```java
// AppConfig.java
factory.setReadTimeout(10000);  // 10 seconds before giving up
```
Worst case per `addPost`: 10s + 10s = **20 seconds** blocked, holding a MySQL connection and an HTTP thread the entire time.

**Evidence — Single-worker uvicorn.**
```
# hate-api/Dockerfile line 84
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
```
One worker. If the backend sends two concurrent `analyzeText` requests, the second one queues behind the first at the uvicorn level. With inference taking ~500ms, **throughput is capped at ~2 posts/second** across the entire platform.

**Call count per operation.**

| Operation | `analyzeText` calls | Max wait |
|---|---|---|
| `addPost` | 2 (title + content) | 20 s |
| `updatePost` | 2 (title + content) | 20 s |
| `createForum` | 2 (name + description) | 20 s |
| `updateForum` | 2 (name + description) | 20 s |
| Add comment | **0** — not checked at write time | 0 |
| Scheduler per comment | 1 | 10 s |

Note: **comments are not moderated at write time** (no `analyzeText` in `CommentService.addComment()`), but are re-scanned nightly by the scheduler. This is inconsistent: posts are blocked synchronously, comments slip through immediately and are cleaned up hours later (see HS-04).

**Impact.**
1. **User experience:** Posting takes 0.5–2 seconds longer than it should. On slow days (model warm-up, GC pause), it can timeout entirely, showing the user an error even though their content was fine.
2. **Throughput ceiling:** A single uvicorn worker with CPU inference caps the platform at ~2 content writes/second globally, regardless of backend scaling.
3. **MySQL connection starvation:** The call is inside `@Transactional`. A connection pool of 10 connections means only 10 concurrent posts are possible before new requests start queuing (each post holds its connection for the full inference duration). *(See PERF-02, PERF-NEW-01 for the connection-pool-sizing side of this same defect.)*
4. **False sense of correctness:** Comments bypass the synchronous check entirely, meaning moderation is inconsistent by design.

**Recommended Designs.**

**Option A — Post-Publish Async Moderation (Recommended)**
```
User submits post
    │
    ▼
PostService.addPost() ──→ Persist post with status = PENDING_REVIEW
    │                      Return 202 Accepted immediately
    │
    ▼ (async, separate thread pool or job queue)
ModerationWorker.moderateAsync(postId)
    ├── Call hate-api
    ├── If HATE:  soft-delete post + notify user
    └── If SAFE:  update status = PUBLISHED, now visible in feed
```
Benefits: user sees immediate 202 response (optimistic UX, like Reddit/Twitter); moderation is decoupled from the write path; hate-api failure only delays publication, never blocks the user. Trade-off: content is briefly visible in a "pending" state — for a social platform this is standard practice.

**Option B — Synchronous but Batched (quick win, keep sync UX).** Combine title + content into a single call instead of two:
```java
// Current (2 calls): analyzeText(title); analyzeText(content);
// Better (1 call, hate-api already sentence-tokenizes internally):
analyzeText(title + "\n" + content);
```
Cuts HTTP overhead by 50% with zero architectural change. *(See HS-03/HS-07 for the full detail on this option.)*

**Option C — Result Cache (idempotency).** Identical text (same edit submitted twice, retry on network error) re-runs the model unnecessarily. A Redis cache keyed on `SHA256(text)` with a short TTL (1 hour) would eliminate duplicate inference for the common retry case.

---

### HS-02 — `@Transactional` Held Open During Inference (Connection Pool Exhaustion)

**Severity:** High | **Effort to fix:** Low

**Description.** `PostService.addPost()` and `ForumService.createForum()` are annotated `@Transactional`. The MySQL connection is acquired at the start of these methods and held until they return. The `analyzeText` HTTP call sits in the middle of this transaction, holding the connection for the full inference duration.

**Evidence.**
```java
// PostService.java line 43
@Transactional
public Post addPost(AddPostDto addPostDto, Long userId) {
    Forum forum = mongoTemplate.findById(...);  // no MySQL here — Mongo
    if (!hateSpeechService.analyzeText(content) || ...) { // HTTP call — 500ms+
        ...
    }
    forum.setPostCount(forum.getPostCount() + 1);
    forumRepository.save(forum);               // ← this is Mongo too
    return postRepository.save(post);          // ← this is Mongo too
}
```
`addPost` operates entirely on MongoDB. The `@Transactional` annotation has **no effect** on MongoDB (Spring's `@Transactional` only covers the JPA/MySQL datasource unless MongoDB multi-document transactions are explicitly configured). The annotation is therefore meaningless here AND the HTTP call is not inside a real transaction boundary for the data being written.

**Impact.** `@Transactional` gives the false impression of atomicity on MongoDB operations that are not actually transactional. The annotation *does* affect the `@Async` task executor in some configurations, potentially causing subtle issues if async operations are launched from within.

**Recommended Fix.** Remove `@Transactional` from `PostService.addPost()` and `ForumService.createForum()` — they operate on MongoDB, not MySQL. Move the `analyzeText` call to before any database interaction (move it to the controller or a pre-validation step).

---

### HS-03 — The Model Is Called Once Per Sentence Field, Not Once Per Content Unit

**Severity:** Medium | **Effort to fix:** Low

**Description.** For `addPost`, `analyzeText` is called twice: once for `title`, once for `content`. The hate-api already performs its own sentence tokenization via NLTK. A single call with `"title\ncontent"` would produce the same result with half the HTTP overhead.

**Evidence.**
```java
// PostService.java line 52
if (!hateSpeechService.analyzeText(addPostDto.getContent())
 || !hateSpeechService.analyzeText(addPostDto.getTitle())) {
```
```python
# hate-api/app.py line 33
def analyze_text(text):
    sentences = nltk.sent_tokenize(text)   # already splits into sentences
    for s in sentences:
        ...
```

**Impact.** Double the HTTP calls, double the latency, double the uvicorn queue pressure, for no correctness benefit. A title of "I hate [group]" followed by safe content is correctly flagged whether they are sent together or separately.

**Recommended Fix.**
```java
// One call:
String combined = addPostDto.getTitle() + "\n" + addPostDto.getContent();
if (!hateSpeechService.analyzeText(combined)) {
    throw new HateSpeechException("hate speech detected");
}
```

---

### HS-04 — Comments Are Not Moderated at Write Time (Inconsistency)

**Severity:** High | **Effort to fix:** Low

**Description.** `CommentService.addComment()` does not call `analyzeText`. Comments are written immediately and only cleaned up by the nightly scheduler — up to 24 hours later. Posts and forums are blocked synchronously; comments are not. This is an inconsistent and insecure design.

**Evidence.**
```java
// CommentService.java — addComment() lines 32-50
public Comment addComment(Long ownerId, AddCommentDTO addCommentDTO) {
    canComment(postId);
    ...
    return commentRepository.save(comment);   // ← no hate-speech check
}
```
vs.
```java
// PostService.java line 52
if (!hateSpeechService.analyzeText(...))
    throw new HateSpeechException("hate speech detected");
```

**Impact.** Hateful comments are live for up to 24 hours. Comments are the highest-volume content type on a social platform — skipping moderation here while enforcing it on posts creates a clear bypass: post hate speech as a comment, not a post.

**Recommended Fix.** Either apply the same async post-publish moderation to comments (recommended — see HS-01 Option A), or add a synchronous check: `analyzeText(content)` in `addComment()` before saving.

---

### HS-05 — The hate-api `app.py` Is Production-Unready

**Severity:** High | **Effort to fix:** Low–Medium

The FastAPI application has several critical issues:

**5a. `print(request.text)` Logs Every User Message to Stdout**
```python
# app.py line 43
@api_router.post("/analyze")
def analyze(request: TextRequest):
    print(request.text)   # ← logs every analyzed string
    return analyze_text(request.text)
```
This is a debug statement that was never removed. In production it logs every post, comment, and forum name to stdout verbatim — a privacy leak, since server access logs now contain user-generated content in unstructured form alongside framework logs. *(Same defect as SEC-07.)*

**5b. NLTK Downloads at Startup (Blocks First Request)**
```python
# app.py lines 8-9
nltk.download('punkt')
nltk.download('punkt_tab')
```
This runs at **module load time** on every cold start. NLTK downloads are I/O operations that block startup. In the Dockerfile, NLTK data is pre-downloaded to `/nltk_data` and `NLTK_DATA=/nltk_data` is set — so these runtime downloads are redundant and hit the network unnecessarily on startup if the path lookup fails.

**5c. Model Loaded at Module Level — No Warmup Endpoint**
```python
# app.py lines 13-14
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSequenceClassification.from_pretrained(model_name)
```
The model loads at startup (correct), but there is no warmup inference run. The first real request after startup will be slower due to PyTorch JIT compilation caching. The Dockerfile `HEALTHCHECK` only hits `/health` — which does not run inference — so the container is declared healthy before it has actually processed its first inference request.

**5d. No Input Validation or Length Limit**
```python
class TextRequest(BaseModel):
    text: str   # no max_length, no validator
```
A request with a 1 MB string will run NLTK tokenization and N model inferences on that string (one per sentence, potentially hundreds). There is no timeout on the inference itself and no limit on input size, making the service trivially denial-of-serviceable. *(Also directly related to ARCH-NEW-01/SEC-NEW-01 — this is what "zero input validation" looks like on the Python side of the stack too.)*

**5e. Single-Threaded Inference with `--workers 1`**
```
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
```
PyTorch model inference is not thread-safe for parallel calls on the same model instance without locking. The `--workers 1` setting is technically correct for safety but creates the throughput ceiling identified in HS-01. Alternatives: **multiple workers + model per worker** (`--workers 4`, 4× RAM usage, 4× throughput); **async endpoint with semaphore** (one model instance, async FastAPI endpoint, semaphore to serialize inference while allowing concurrent I/O); **GPU** (CPU inference on RoBERTa takes 200–500ms; with a small GPU it drops to <20ms).

**5f. `analyze_text` Returns `None` on Safe Text (Implicit Return)**
```python
def analyze_text(text):
    sentences = nltk.sent_tokenize(text)
    for s in sentences:
        label, prob = classify_sentence(s)
        if label == labels[1]:
            return False   # hate detected
    return True            # safe
```
This is correct Python but the endpoint returns the raw boolean:
```python
@api_router.post("/analyze")
def analyze(request: TextRequest):
    return analyze_text(request.text)
```
FastAPI serializes `True` as the JSON value `true` and `False` as `false`. The Java client parses this as `Boolean`. If the `text` field is empty (`""`), `nltk.sent_tokenize("")` returns `[]`, the loop doesn't execute, and the function returns `True` — so empty strings are always classified as safe. This is correct behavior, but it is implicit and fragile.

**5g. No API Authentication**
```python
# No dependency, no header check, no API key validation
@api_router.post("/analyze")
def analyze(request: TextRequest):
    ...
```
Any request that reaches the service is processed. The Docker Compose file previously exposed port 8000 on the host, making it publicly reachable. *(Same underlying issue as SEC-07 — this is the application-layer half of it, SEC-07 is the network-layer half.)*

**Recommended fix summary for `app.py`:**
```python
import os, logging
from fastapi import FastAPI, APIRouter, Depends, HTTPException, Header
from pydantic import BaseModel, Field
from functools import lru_cache
import asyncio

logger = logging.getLogger(__name__)  # structured logging, not print()

INTERNAL_API_KEY = os.environ.get("HATE_API_KEY", "")
MAX_TEXT_LENGTH = 10_000  # characters

class TextRequest(BaseModel):
    text: str = Field(..., max_length=MAX_TEXT_LENGTH)

def verify_api_key(x_internal_api_key: str = Header(...)):
    if not INTERNAL_API_KEY or x_internal_api_key != INTERNAL_API_KEY:
        raise HTTPException(status_code=401, detail="Invalid API key")

_inference_lock = asyncio.Lock()

@api_router.post("/analyze", dependencies=[Depends(verify_api_key)])
async def analyze(request: TextRequest):
    async with _inference_lock:           # serialize inference safely
        result = analyze_text(request.text)
    logger.info("Analyzed text, result=%s, length=%d", result, len(request.text))
    return result

@api_router.get("/health")
async def health():
    # Run a warmup inference to verify model is working
    try:
        analyze_text("This is a test.")
        return {"status": "ok"}
    except Exception as e:
        return {"status": "error", "detail": str(e)}
```

---

### HS-06 — Nightly Scheduler Is an O(N) Sequential HTTP Bomb

**Severity:** High | **Effort to fix:** Medium

**Description.** `HateSpeechScheduler.schedule()` runs at midnight and re-scans every comment created in the last 24 hours, one HTTP request per comment, sequentially.

**Evidence.**
```java
// HateSpeechScheduler.java
List<Comment> todayComments = commentRepository.findAllByCreatedAtBetween(yesterday, now);
for (Comment comment : todayComments) {
    cleanComment(comment);               // → analyzeText() → HTTP call → up to 10s
}
```

**Calculation.**

| Daily comments | Sequential time (500ms avg) | At 10s timeout |
|---|---|---|
| 1,000 | ~8 min | ~2.8 h |
| 10,000 | ~83 min | ~28 h |
| 50,000 | ~7 h | already impossible |

At 10,000 comments/day the scheduler cannot finish before the next midnight trigger — the second run starts while the first is still running, creating a runaway task pile-up.

**Additional issues.**
1. **Full collection load into heap:** `findAllByCreatedAtBetween` returns a `List<Comment>`. At 50,000 comments/day this is significant heap pressure. *(Same defect as PERF-04.)*
2. **Re-scans already-moderated content:** Comments that passed the nightly scan last night are included again tonight if they were created within the window. There is no "moderated" flag to skip already-processed comments.
3. **Scanning comments only:** Posts and forums are moderated synchronously at write time; comments are not (HS-04). The scheduler is a patch for missing write-time moderation, not a feature.

**Recommended Fix.**
1. Add `isModerated: Boolean` (default `false`) to `Comment`.
2. Write-time moderation (async, HS-01 Option A) sets it to `true` after checking.
3. The scheduler becomes a safety net that **only** scans `isModerated = false` comments — a small fraction on any healthy day.
4. Use paginated streaming instead of full list load.
5. Use a thread pool (`@Async` with a bounded executor) to parallelize HTTP calls.

---

### HS-07 — Short-Circuiting `||` Causes Inconsistent Moderation Results

**Severity:** Medium | **Effort to fix:** Low

**Description.** The hate-speech check uses the `||` (short-circuit OR) operator with negations:
```java
if (!analyzeText(content) || !analyzeText(title)) {
    throw new HateSpeechException(...);
}
```
Due to short-circuit evaluation: if `content` is hateful (`analyzeText` returns `false`, `!false = true`), the `title` is **never analyzed**. This is logically correct (we only need one hateful field to reject), but it creates a problem when hate-api is slow or unavailable:
- If `content` call times out and throws `RestClientException`, the `analyzeText` catch block returns `true` (fail-open). `!true = false`. The `||` continues and checks `title`.
- If `title` also times out, it also returns `true`. `!true = false`. The overall condition is `false || false = false` — content is **allowed**.

This is the intended fail-open behavior, but it means both fields must independently fail for the content to be allowed. A partial failure (content times out, title is safe) still results in the content being allowed — which is the correct outcome, but the code makes this reasoning very hard to follow.

**Recommended Fix.** Extract the logic for clarity:
```java
boolean contentSafe = hateSpeechService.analyzeText(combined); // single combined call
if (!contentSafe) {
    throw new HateSpeechException("hate speech detected");
}
```
With a single combined call (title + content), the short-circuit ambiguity disappears.

---

### HS-08 — Incomplete JSON Escaping in HTTP Body Construction

**Severity:** Medium | **Effort to fix:** Low

**Description.** `HateSpeechService.analyzeText()` builds the JSON request body by hand:
```java
// HateSpeechService.java line 34
String body = "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
```
The escaping handles `\` and `"`, but misses: newlines (`\n` → must become `\\n` in JSON strings), carriage returns (`\r`), tabs (`\t`), null bytes (`\0`), and other control characters (U+0000–U+001F). *(Same defect as CQ-01.)*

**Impact.** A comment like:
```
This is a great movie!
I love it.
```
Contains a literal newline. The generated JSON:
```json
{"text":"This is a great movie!
I love it."}
```
Is **invalid JSON**. The FastAPI endpoint's Pydantic model will reject it with a 422 Unprocessable Entity. Since the Java catch block catches `RestClientException` (which includes HTTP 4xx from Spring's `RestTemplate` — 4xx responses throw `HttpClientErrorException`, a subtype of `RestClientException`), the 422 will be caught, and the content will be allowed through (fail-open). **This means multi-line posts/comments bypass hate-speech moderation entirely.**

**Recommended Fix.** Use Jackson:
```java
// Inject ObjectMapper (already available via Spring Boot)
private final ObjectMapper objectMapper;

public boolean analyzeText(String text) {
    try {
        Map<String, String> body = Map.of("text", text);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, buildHeaders());
        ResponseEntity<Boolean> response = restTemplate.postForEntity(url, request, Boolean.class);
        return Boolean.TRUE.equals(response.getBody());
    } catch (RestClientException e) {
        log.warn("Hate-speech service unavailable, failing open: {}", e.getMessage());
        return true;
    }
}
```

---

### Summary Table (hate-api sub-findings)

| ID | Finding | Severity | Effort |
|---|---|---|---|
| HS-01 | Synchronous blocking moderation on every write | Critical | Medium |
| HS-02 | `@Transactional` held during HTTP call (fake atomicity + connection waste) | High | Low |
| HS-03 | 2 HTTP calls per operation instead of 1 (title + content separately) | Medium | Low |
| HS-04 | Comments not moderated at write time — up to 24h exposure | High | Low |
| HS-05a | `print(request.text)` — privacy leak in production | High | Low |
| HS-05b | NLTK download at startup — blocks cold start | Low | Low |
| HS-05c | No warmup inference — healthcheck passes before model is ready | Medium | Low |
| HS-05d | No input length limit — DoS via oversized text | High | Low |
| HS-05e | Single worker — global throughput capped at ~2 writes/second | High | Medium |
| HS-05g | No API authentication on the inference endpoint | High | Low |
| HS-06 | Nightly scheduler is O(N) sequential at 10s/call — unfinishable at scale | High | Medium |
| HS-07 | Short-circuit `\|\|` makes fail-open logic hard to reason about | Medium | Low |
| HS-08 | Incomplete JSON escaping — newlines in content bypass moderation | High | Low |

### Recommended Target Architecture

```
                   ┌─────────────────────────────────────┐
                   │           Backend                   │
                   │                                     │
  POST /post ─────►│ 1. Validate inputs                  │
                   │ 2. Save post (status=PENDING)        │──► MongoDB
                   │ 3. Return 202 Accepted immediately  │
                   │ 4. Submit ModerationJob async        │
                   │    (Spring TaskExecutor or Redis Q) │
                   └─────────────────────────────────────┘
                                    │ async
                                    ▼
                   ┌─────────────────────────────────────┐
                   │       ModerationWorker              │
                   │  - Combines title + content         │
                   │  - Calls hate-api (1 request)       │
                   │  - On SAFE: status=PUBLISHED        │
                   │  - On HATE: status=REJECTED,        │
                   │    soft-delete, notify user          │
                   │  - On ERROR: retry up to 3×,        │
                   │    then status=PUBLISHED (fail-open)│
                   └─────────────────────────────────────┘
                                    │
                                    ▼
                   ┌─────────────────────────────────────┐
                   │         hate-api (FastAPI)          │
                   │  - Auth: X-Internal-Api-Key header  │
                   │  - Input: max 10,000 chars          │
                   │  - Single combined text field        │
                   │  - Async endpoint + inference lock  │
                   │  - Proper logging (not print)       │
                   │  - Healthcheck runs real inference  │
                   │  - Not exposed on host network       │
                   └─────────────────────────────────────┘
```

This design removes all moderation latency from the user-facing request path, eliminates connection-pool pressure, handles failures with retry + eventual fail-open, applies the same strategy consistently to posts, comments, and forums, allows hate-api to be scaled independently (multiple workers, GPU upgrade), and makes the scheduler redundant (or a simple cleanup for edge cases only).

> **One addition from the independent follow-up review that the analysis above didn't have visibility into, because it requires actually building the container rather than reading `app.py`:** see §11.1/SEC-NEW-06 — the Dockerfile's silent disregard for the pinned `requirements.txt`, which is a second, independent production-readiness blocker for this service beyond everything documented above.

---

## 14. Testing & Observability Review

### TEST-01 — `@SpringBootTest` + `@BeforeEach` Mock Override Anti-Pattern

**Severity:** High | **Effort to fix:** Medium

**Description.** `UserServiceTest` is annotated `@SpringBootTest` (which loads the full application context), but its `@BeforeEach` immediately **replaces** the autowired beans with manual Mockito mocks. This means the full Spring context is loaded on every test run (slow, requires DB connections) and then discarded in favour of hand-wired mocks — combining the worst of both worlds.

**Evidence.**
```java
// UserServiceTest.java lines 31-60
@SpringBootTest(classes = {BackendApplication.class, SecurityConfig.class})
@ActiveProfiles("test")
class UserServiceTest {
    @Autowired private UserRepository userRepository;  // Spring-wired

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);   // immediately thrown away
        ...
        userService = new UserService(userRepository, mongoTemplate);  // manual wiring
    }
```

**Impact.** Test startup takes 10–30 seconds to load the Spring context, then wastes it. Tests are fragile: the manual `new UserService(...)` constructor must match the class's actual constructor exactly. Any new `final` field in `UserService` will break all tests silently (Lombok will add it to the constructor but the test still calls the 2-arg version). The `@Transactional` on the test class has no effect since no real transaction is used.

> **Independently re-verified.** `UserServiceTest.java` was read directly during the follow-up review and this description matches the actual source verbatim. A broader sweep found this pattern in **7 of 57** backend test files — the other 50 correctly use lighter-weight patterns (`@ExtendWith(MockitoExtension.class)` or Testcontainers-backed integration tests). So this finding is real, but should be read as "a recurring anti-pattern in a minority of files," not "the whole suite is like this." Worth stating plainly: **the backend test suite is not thin** — 57 test files touching nearly every controller/service/repository across every module is a genuinely solid baseline. The problem here is test *design* quality in a subset of files, not test *coverage breadth*.

**Recommended Fix.** Replace `@SpringBootTest` with `@ExtendWith(MockitoExtension.class)`. Use `@InjectMocks` for the service and `@Mock` for dependencies. This is 10x faster and correctly tests the service in isolation:
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private MongoTemplate mongoTemplate;
    @InjectMocks private UserService userService;
    ...
}
```

---

### TEST-02 — No Tests for Critical Security Paths

**Severity:** High | **Effort to fix:** Medium

**Description.** The test suite has no tests covering: JWT token validation edge cases (expired token, tampered signature, missing claims); the `InternalApiKeyFilter` (watch-party auth filter) behavior when key is missing vs. present vs. wrong; the hate-speech moderation path being bypassed when the service is unavailable; concurrent vote/comment operations (race conditions identified in REL-01).

**Evidence.** `SecurityIntegrationTest.java` exists but is the only security integration test. `HateSpeechServiceTest.java` exists but likely only unit-tests the service call, not the fail-open behavior. `WatchPartyApplicationTests.java` in the watch-party module is a placeholder (Spring Boot test that only checks context loads).

**Impact.** Critical security behaviors are untested and can regress silently.

**Recommended Fix.** Add: `JWTProviderTest` (test expired token rejection, tampered token rejection, missing `role` claim); `InternalApiKeyFilterTest` (test 401 on missing/wrong key, 200 on correct key, health bypass — including the `startsWith` bug in SEC-NEW-04); `HateSpeechIntegrationTest` (test that a `RestClientException` from hate-api results in fail-open); `VoteConcurrencyTest` (use `ExecutorService` with 10 threads to verify vote counts are correct after concurrent voting — this is exactly the kind of test that would have caught REL-01/REL-NEW-01 before they shipped).

---

### TEST-03 — `ForumPostCommentIntegrationTest` Is the Only End-to-End Test

**Severity:** Medium | **Effort to fix:** High

**Description.** The integration test suite has one actual integration test (`ForumPostCommentIntegrationTest`) covering the forum → post → comment flow. All other tests are service-layer unit tests with mocks.

**Evidence.** `ForumPostCommentIntegrationTest.java` (uses `AbstractMongoIntegrationTest` with Testcontainers). No integration tests for: authentication flows, watch-party lifecycle, movie ratings, user following, or the cascade deletion service.

**Impact.** The cascade deletion logic (`CascadeDeletionService`, 441 lines) has no integration test. A regression in the async graph-lookup aggregation would not be caught. The `@Async` methods in `CascadeDeletionService` and `UserService` are particularly hard to verify without integration tests (the async call completes after the test method returns).

**Recommended Fix.** Add integration tests for cascade deletion using Testcontainers (MongoDB is already containerized in `AbstractMongoIntegrationTest`). Add a `@Async` integration test that uses `CompletableFuture` and waits for async completion with a timeout. Add watch-party lifecycle integration tests using the in-memory Redis option (`spring-data-redis` embedded).

---

### TEST-04 — No Tests for the Watch-Party Microservice

**Severity:** Medium | **Effort to fix:** Medium

**Description.** The `watch-party` module contains only `WatchPartyApplicationTests.java` — a single Spring Boot context load test with no assertions.

**Evidence.**
```
/watch-party/src/test/java/org/example/watchparty/WatchPartyApplicationTests.java
# Only file in the test directory
```

**Impact.** `WatchPartyService` (346 lines) including `createParty`, `joinParty`, `leaveParty`, and `deletePartyInternal` are entirely untested. The N+1 Redis read in `getPartyMembers` and the non-atomic count update (REL-06/REL-NEW-01) are invisible without tests.

**Recommended Fix.** Use Testcontainers with `redis:7` to spin up a real Redis instance for the tests. Test: party creation and retrieval; join/leave with duplicate prevention; party auto-deletion when participant count reaches 0; TTL expiry behavior (use a short test TTL); and — given REL-NEW-01's precise race trace — a concurrent-join test using an `ExecutorService` with multiple threads hitting `joinParty` for the same user simultaneously.

---

### TEST-05 — No Observability / Metrics

**Severity:** High | **Effort to fix:** Medium

**Description.** The application has no metrics, no distributed tracing, and no health indicators beyond a basic HTTP ping.

**Evidence.** `pom.xml`: no `spring-boot-starter-actuator` dependency (independently re-confirmed by reading `pom.xml` directly). `HealthController.java` returns a static `{ "status": "UP" }` — no DB connectivity check, no Redis connectivity check, no hate-api reachability check. No Micrometer, Prometheus, or OpenTelemetry dependency anywhere in either service.

**Impact.** In production, there is no way to know: how many requests per second each endpoint handles; what the hate-api call latency/failure rate is; whether Redis is connected and healthy; JVM heap usage or GC pressure. Incidents can only be detected by users reporting errors, not proactively by alerts. *(Compounded by CQ-NEW-01 — even the one place a 500 error surfaces, `GlobalExceptionHandler`, doesn't log it.)*

**Recommended Fix.**
1. Add `spring-boot-starter-actuator` and expose `/actuator/health` with database health indicators (`DataSourceHealthIndicator`, `MongoHealthIndicator`, `RedisHealthIndicator`).
2. Add `micrometer-registry-prometheus` and expose `/actuator/prometheus`.
3. Instrument the hate-api call with a timer: `meterRegistry.timer("hatespeech.analyze").record(...)`.
4. Add a Prometheus + Grafana container in `compose.yaml` for local monitoring.

---

### TEST-06 — Logging Is Not Structured

**Severity:** Medium | **Effort to fix:** Low

**Description.** All services log using default Logback pattern format (human-readable text). In a containerized environment where logs are collected by a log aggregation system (e.g., ELK, Loki), unstructured logs are difficult to query and correlate.

**Evidence.** No `logstash-logback-encoder` or JSON log appender dependency in either `pom.xml`. Typical log line: `2024-06-09 12:00:00 INFO CommentService - Error during comment votes deletion: ...` — no `partyId`, `userId`, or `requestId` context.

**Impact.** Cannot search logs by `userId` or `partyId` in a log aggregator without regex parsing. Cannot correlate a user-reported error with a specific request without a trace ID.

**Recommended Fix.**
1. Add `logstash-logback-encoder` and configure JSON output in `logback-spring.xml`.
2. Add an MDC filter that injects `requestId`, `userId`, and `userRole` into every log line for authenticated requests.

---

### TEST-07 — `CleanupScheduler` Has No Alerting on Failure

**Severity:** Low | **Effort to fix:** Low

**Description.** `VerificationCleanupScheduler` and `HateSpeechScheduler` run on `@Scheduled` crons. If they throw an uncaught exception, Spring swallows it silently and the scheduler continues on the next trigger.

**Evidence.**
```java
// HateSpeechScheduler.java
@Scheduled(cron = "0 0 0 * * ?")
public void schedule() {
    cleanHateSpeachComments();  // no try-catch, no success/failure metric
}
```

**Impact.** If the nightly hate-speech scan fails (e.g., MongoDB is unreachable), no alert is raised. Hate speech comments accumulate silently. Verification records are not cleaned up if the scheduler fails → table grows unbounded.

**Recommended Fix.** Wrap scheduler bodies in `try-catch` with `log.error()`. Emit a counter metric on failure (`meterRegistry.counter("scheduler.failure", "name", "hateSpeech").increment()`). Consider a Spring `@Scheduled` error handler configuration.

---

## 15. Cross-Audit Reconciliation

This section documents how the original nine-document audit and the independent follow-up review were compared, where they agreed, what the follow-up added, and how the two direct disagreements that surfaced were resolved with evidence rather than by picking a side.

### 15.1 Where both audits agree (strong signal — treat as settled)

Every one of the original audit's 53 findings was checked against the actual source during the follow-up review (either directly or by a sub-agent working independently from the same source tree) and **none were found to be inaccurate**. Several were independently re-derived from scratch with no access to the original audit's text, which is a meaningfully stronger form of confirmation than "I re-read the same file and agreed": the vote/comment counter race (REL-01 ≈ PERF-NEW-02), the watch-party MySQL/Redis split-brain (REL-02), the WebSocket auth gap (REL-08 ≈ SEC-NEW-02), the participant-count non-atomicity (REL-06 ≈ REL-NEW-01), and the `InternalApiKeyFilter` fail-open behavior (SEC-08) were all rediscovered independently with the same root cause and, in most cases, the same recommended fix. Treat these as the highest-confidence findings in this combined document.

### 15.2 What the follow-up review added

The original audit is strong on architecture, reliability, and the hate-api deep-dive, and comparatively light on: (a) API-surface-wide input validation, (b) unbounded/unpaginated endpoints as a systemic pattern rather than a one-off, (c) anything that required actually *running* the system rather than reading it, and (d) CI/CD and dependency-hygiene as a category. Concretely new:

- **ARCH-NEW-01 / SEC-NEW-01** — near-total absence of Bean Validation across the API (2 of 28 DTOs), which is arguably the single highest-leverage fix in this entire report given how many downstream problems (bad data reaching Mongo/MySQL, unhandled validation exceptions falling into a 500) trace back to it.
- **API-NEW-01** — unbounded list endpoints on admin/org dashboards (distinct from PERF-04, which only covers the nightly scheduler case of the same underlying pattern).
- **CQ-NEW-02** — the Jackson serialization name collision on `Movie.organization`, likely to affect the platform's single most-used public endpoint.
- **SEC-NEW-02** (partyId injection + wildcard CORS on the WebSocket) and **SEC-NEW-03** (timing-unsafe key comparison) — both verified against actual source, both real, neither in the original SEC/REL series.
- **§11.1** — the hate-api Dockerfile/`requirements.txt` mismatch, found only by building and running the container, which is structurally outside what a code-reading audit can discover. This is the strongest argument in this entire document for *why* a hands-on verification pass adds value beyond a second static read.
- **§11.2–11.6** — CI/CD gaps, the `JPA_DDL_AUTO` dev/prod disagreement, missing resource limits, missing backups, and the `DataInitializer` profile-unreachability bug. ARC-08/TEST-05 covered observability well but treated "DevOps" narrowly as "add Prometheus" — the follow-up found five more concrete, independent gaps in the same category.
- **DB-NEW-01 through DB-NEW-04** — schema-level findings (JPA cascade footgun, Vote schema/index mismatch, soft-delete inconsistency) that sit below the architectural level the original ARC-series operates at.

### 15.3 Where a direct contradiction surfaced, and how it was resolved

One direct contradiction surfaced during independent verification, worth documenting in detail because it's a good demonstration of *why* claims need re-verification rather than being taken on faith from either source:

**CQ-10 (`Movie.ratingCount` initialized to 1 instead of 0) — the original audit says this is a real bug; a parallel automated reviewer working independently during the follow-up claimed it was "not confirmed" and that the logic in `MovieReviewService` "increments properly."** The actual data flow was traced by hand to adjudicate: `Movie.onCreate()` (`@PrePersist`) sets `ratingCount = 1` (not `0`) whenever it's null — which for a freshly created movie means it becomes non-null-but-wrong *before* any review exists. `MovieReviewService.addOrUpdateReview()` then has a guard, `if (movie.getRatingCount() == null) movie.setRatingCount(0)`, which the contradicting claim read as protecting against exactly this — but the guard never fires, because by the time a review is added, `ratingCount` is already `1`, not `null`. So the first real review does `ratingCount = 1 + 1 = 2`, and `averageRating = ratingSum / 2` instead of `/ 1` — precisely the `5-star review shows as 2.5` bug the original audit describes. **Verdict: the original audit (CQ-10) is correct; the contradicting claim was wrong**, and the error came from checking the null-guard in isolation without tracing what value the field actually holds by the time that guard runs. This is now the single most rigorously verified finding in this document, and it's a fairly severe one — it's a core-feature (movie rating) correctness bug, not an edge case.

### 15.4 Where the two reviews have a materially different opinion on severity

- **DB-NEW-01 (User JPA cascade)**: an initial automated pass on this finding rated it **Critical**, reasoning that any future hard-delete would be catastrophic. The final rating in this document is **Medium**, having verified that no code path in the current system actually triggers a hard delete of a `User` — the risk is real and worth fixing before it becomes load-bearing, but "will corrupt data the day someone adds a feature that doesn't exist yet" and "is corrupting data right now" are different severities, and conflating them either over-alarms a reader trying to triage 91 findings, or (worse) trains the reader to distrust "Critical" labels generally, which defeats the purpose of having them. The recommendation — fix it now, before the feature exists — is unchanged; only the urgency label moved.
- **ARC-02 (watch-party premature extraction)**: the original audit rates this Medium. Having read the full module (not just the service class) during the follow-up, this document leans toward treating it as effectively High in combination with SEC-NEW-02/SEC-NEW-03/REL-NEW-01 — the *architectural* decision to extract might be a reasonable Medium-severity concern on its own, but the *current state* of the extracted service (unauthenticated control channel + client-controlled Redis keys + non-atomic state + near-zero tests) means the boundary that was drawn is currently the least-defended part of the whole system. Whoever picks up ARC-02 should treat "fix the watch-party service's security/concurrency bugs" and "decide whether it should be a separate service at all" as the same ticket, not two — fixing the bugs in place is strictly necessary regardless of which architectural direction is chosen.

### 15.5 Good observations from the original audit that this document specifically endorses

The original audit's "Positive Observations" (health checks, soft-delete pattern, the `InternalApiKeyFilter` concept being structurally sound even with a fail-open bug, MongoDB compound indexes, the explicitly-documented fail-open hate-speech design, `AccessService` centralization, Testcontainers usage, batched cascade deletion, multi-stage Dockerfiles, env-var-driven CORS — reproduced in full in §16.3) all independently held up under the follow-up review's own pass through the same code, and are worth a reader's attention precisely because a 91-finding document risks reading as uniformly negative. It is not; several of these patterns (batched async cascade deletion in particular) are better than what a lot of production codebases at this team size actually ship.

---

## 16. Original Audit Digest (Top Changes, Hotspot Files, Positive Observations)

*(This section preserves, in full, the executive-digest material from the original audit's `findings-summary.md` — the top-10 impact ranking, the file hotspot table, and the positive observations list — since it captures a distinct angle (impact ranking, file-level clustering) that the section-by-section review above doesn't surface on its own. Where original text pointed to solutions, the corresponding IDs used throughout this document have been kept intact for cross-reference.)*

### 16.1 Top 10 Highest-Impact Changes (as originally ranked)

1. **🔴 Fix Watch-Party State Consistency (REL-02, ARC-01).** Move `callMicroserviceInitialize()` outside `@Transactional`. Add compensating delete on failure. This prevents permanently orphaned MySQL party records. *Effort: Low | Impact: Critical reliability fix.*
2. **🔴 Fix Production Docker Image (`.env.production`) (FE-02, FE-03).** Set real production URLs in `.env.production`. Proxy WebSocket connections through nginx rather than directly connecting the browser to the internal watch-party container. *Effort: Low | Impact: The application cannot work in production without this.*
3. **🔴 Replace Verification Codes With UUID Tokens + Rate Limiting (SEC-03, ARC-04).** Replace 6-digit int codes with `SecureRandom` UUID tokens. Add 5-attempt lockout. Store hash not plaintext. *Effort: Low | Impact: Eliminates account-takeover-via-enumeration attack.*
4. **🔴 Add Authentication to WebSocket Control Channel (REL-08).** Validate JWT in the STOMP `CONNECT` frame via a `ChannelInterceptor`. Verify sender is a party member before broadcasting. *Effort: Medium | Impact: Prevents unauthenticated disruption of any watch party.*
5. **🔴 Fix Vote/Comment Counter Race Conditions (REL-01, REL-06).** Replace read-modify-write patterns with MongoDB `$inc` / Redis `HINCRBY` atomic operators. *Effort: Low | Impact: Data integrity — vote counts are currently lossy under any concurrent load.*
6. **🟠 Fix Verification Code Not Deleted After Password Reset (REL-04).** Add `verificationRepository.delete()` in `updatePasswordByVerificationCode()`. One-time codes must be consumed on use. *Effort: Low | Impact: One-time codes are currently reusable within the 10-minute window.*
7. **🟠 Fix `commentCount` Decrement Off-by-One on Cascade Delete (REL-07).** Decrement parent's `numberOfReplies` by 1 (direct child), not by `totalComments` (entire subtree). *Effort: Low | Impact: Reply counts become permanently negative, corrupting UI.*
8. **🟠 Remove hate-api Host Port Exposure and Debug `print()` (SEC-07).** Remove `ports: "8000:8000"` from compose. Remove `print(request.text)` from `app.py`. *Effort: Low | Impact: Privacy + closes direct external access to the ML model.*
9. **🟠 Fix `InternalApiKeyFilter` to Fail-Closed (SEC-08).** Reject requests with 503 when `WATCHPARTY_KEY` is not configured, rather than allowing all requests. *Effort: Low | Impact: Prevents undetected misconfiguration from opening the internal API.*
10. **🟠 Move HTTP Calls Outside `@Transactional` Boundaries (PERF-02).** All `callMicroservice*()` methods are inside `@Transactional` blocks, holding DB connections for up to 10 s. *Effort: Low | Impact: Prevents connection pool exhaustion under moderate traffic.*

### 16.2 Files With the Most Issues

| File | Issues |
|---|---|
| `backend WatchPartyService.java` | REL-02, PERF-02, ARC-02 |
| `VerificationService.java` | REL-04, REL-05, CQ-02, SEC-10 |
| `CascadeDeletionService.java` | REL-07, TEST-03, CQ-NEW-05 |
| `VoteService.java` | REL-01, CQ-06 |
| `CommentService.java` | REL-01, REL-03 |
| `HateSpeechService.java` | CQ-01, SEC-07, HS-08 |
| `HateSpeechScheduler.java` | CQ-11, PERF-04, HS-06 |
| `SecurityConfig.java` | SEC-05, SEC-06 |
| `watch-party WatchPartyService.java` | REL-06, PERF-01, REL-NEW-01, SEC-NEW-02 |
| `frontend/.env.production` | FE-02 |
| `Movie.java` *(added by follow-up)* | CQ-10, CQ-NEW-02, PERF-08 |
| `GlobalExceptionHandler.java` *(added by follow-up)* | CQ-NEW-01, API-NEW-03 |
| `hate-api/Dockerfile` *(added by follow-up)* | SEC-NEW-06, §11.1 |

### 16.3 Positive Observations (from the original audit, all independently reconfirmed — see §15.5)

1. **Docker Compose health checks** — all services have proper health checks with appropriate timeouts.
2. **Soft-delete pattern** — consistent across MongoDB collections with async cascade deletion.
3. **Internal API key filter** — the `InternalApiKeyFilter` concept is correct; only the fail-open default needs fixing.
4. **MongoDB compound indexes** — `Comment` has well-designed compound indexes covering common query patterns.
5. **Hate-speech fail-open design** — explicitly documented and intentional, preventing content blocking during outages.
6. **Separation of `AccessService`** — centralizing authorization logic in a dedicated service is good architecture.
7. **Testcontainers usage** — `AbstractMongoIntegrationTest` shows awareness of proper integration test infrastructure.
8. **Batch processing in `CascadeDeletionService`** — uses a `BATCH_SIZE` constant and processes in chunks, not all at once.
9. **Multi-stage Dockerfiles** — all three services use multi-stage builds correctly.
10. **CORS configuration via environment variables** — the `CORS_ALLOWED_ORIGINS` env-var pattern is clean and deployment-friendly.

---

## 17. Prioritized Action Plan

### Quick Wins (days, not weeks — highest ratio of impact to effort)

1. Fix `frontend/.env.production` real URLs + proxy watch-party WebSocket through nginx (FE-02, FE-03) — **the app cannot be deployed at all without this.**
2. Fix `hate-api/Dockerfile` to actually use `requirements.txt` (§11.1) — one-line change, closes a proven production-blocking defect.
3. Add `MethodArgumentNotValidException`/`IllegalStateException`/`AccessDeniedException` handlers to `GlobalExceptionHandler`, and stop returning raw `ex.getMessage()` on 500s (CQ-NEW-01).
4. Replace 6-digit verification codes with random tokens + rate limiting (SEC-03/ARC-04).
5. Fix vote/comment/participant counters to use atomic `$inc`/`HINCRBY` (REL-01, REL-06, REL-NEW-01).
6. Move `callMicroservice*()` calls outside `@Transactional` (REL-02, PERF-02, HS-02).
7. Fix `JPA_DDL_AUTO` to `validate` in both env templates (§11.2).
8. Fail-closed (not open) in `InternalApiKeyFilter` when `WATCHPARTY_KEY` is unset (SEC-08), and switch its key comparison to constant-time (SEC-NEW-03).
9. Stop accepting a client-supplied `partyId`; always server-generate it (SEC-NEW-02).
10. Restrict `watch-party`'s WebSocket `setAllowedOriginPatterns` to real frontend origins (SEC-NEW-02).
11. Delete the verification record after password reset (REL-04); fix `numberOfReplies` decrement math (REL-07); fix `ratingCount` init to `0` (CQ-10).
12. Remove `ports: 8000:8000` from `hate-api` in compose; remove the debug `print()` (SEC-07).

### Medium-Term Improvements (1-2 sprints)

1. Add Bean Validation to every write-path DTO + `@Valid` on every controller method (ARCH-NEW-01/SEC-NEW-01) — the single highest-leverage item in this whole report.
2. Paginate every list endpoint, including the admin/org dashboards missed by the nightly-scheduler-only PERF-04 (API-NEW-01), and cap max page size globally.
3. Stop returning JPA/Mongo entities directly from controllers; standardize on the `*View`/`*ResponseDTO` pattern everywhere (CQ-NEW-03), which also structurally prevents recurrences of CQ-NEW-02.
4. Introduce typed exceptions (`ResourceNotFoundException`, etc.) and delete the manual try/catch blocks in controllers (CQ-04, API-NEW-03).
5. Add `spring-boot-starter-actuator` + Micrometer/Prometheus, plus structured JSON logging with request-scoped MDC (ARC-08, TEST-05, TEST-06) — and make sure the newly-fixed `GlobalExceptionHandler` (item 3 above, Quick Wins) actually logs, since metrics without log correlation only tells you *that* something broke, not *what*.
6. Add CI workflows for `watch-party`, `hate-api`, and `frontend`; add Dependabot (§11.3).
7. Introduce Flyway and stop relying on `ddl-auto` for schema evolution (§11.2).
8. Add resource limits to every compose service, not just `hate-api` (§11.4); add a documented backup job for MySQL/MongoDB volumes (§11.5).
9. Replace `analyzeText()`'s manual JSON string building with Jackson (CQ-01/HS-08) — this alone fixes the "multi-line content bypasses moderation" bug.
10. Move to async, post-publish moderation for posts/comments/forums consistently (HS-01 Option A) — closes HS-04's inconsistency (comments unmoderated at write time) as a side effect.
11. Fix the remaining Sprint-1/2 items from the original roadmap not already covered above: `REL-03` double-fetch/NPE in `addComment()`; `REL-05` throw (not return empty object) on password-reset email failure; `PERF-05` replace Redis `KEYS` with `SCAN`; `ARC-07` extract a single `IdConverter` utility; `SEC-04` replace JWT-in-URL with a one-time state code for the OAuth redirect; `SEC-01` introduce refresh tokens.

### Long-Term Refactoring Roadmap (quarter-scale)

1. Resolve the watch-party architectural question explicitly (ARC-02/ARCH-NEW-02): either fold it back into the monolith, or commit to it as a real independently-scaled service and fix its security/concurrency posture to match that responsibility — but fix REL-NEW-01/SEC-NEW-02/SEC-NEW-03 regardless of which direction is chosen.
2. Evaluate consolidating MySQL + MongoDB, or at minimum introduce a saga/outbox pattern for the operations that currently span both with no compensation (ARC-01).
3. Consolidate the three-role authentication model (`User`/`Admin`/`Organization` as separate tables) into a single discriminated `accounts` model with a real permission hierarchy (ARC-05).
4. Replace the `Long↔ObjectId` conversion hack (duplicated 6+ times) with a single `IdConverter`, and longer-term, decouple the ID systems entirely rather than encoding one inside the other (ARC-07).
5. Move the Caffeine cache to Redis-backed `RedisCacheManager` before this service is ever scaled to more than one instance (ARC-06) — this is a silent-correctness-bug-in-waiting, not an active one, similarly to DB-NEW-01, and should be fixed on the same "before it's load-bearing" logic.
6. Build out a real observability stack (Prometheus + Grafana + log aggregation) rather than the actuator/metrics-endpoint baseline from the Medium-Term list — the baseline gets you data, this gets you the ability to actually act on it (dashboards, alerting rules, on-call runbooks).
7. Add end-to-end and load testing for the concurrency-sensitive paths (voting, watch-party join/leave) specifically, once the atomic-operation fixes from Quick Wins land — the fixes need regression protection or they'll silently regress back to read-modify-write the next time someone "simplifies" that code.
8. Replace `@SpringBootTest`+manual-mock anti-pattern (TEST-01) codebase-wide with `@ExtendWith(MockitoExtension.class)`; add the security-path tests from TEST-02; add watch-party unit/integration tests (TEST-04); migrate frontend JWT storage from `sessionStorage` to an `HttpOnly` cookie (FE-01); add a Vitest + React Testing Library suite for the frontend (FE-08).

---

## 18. Learning Notes

These are the underlying engineering principles behind the recommendations above, written out explicitly so the patterns generalize past this specific codebase.

**On input validation.** Validation isn't a UX feature ("show a nice error message") — it's a trust-boundary control. Every place external input crosses into your system (an HTTP request body, a WebSocket message, a query parameter) is a place where you either enforce your invariants or inherit whatever invariants the caller happened to have (none, usually). The fact that 27 of 28 DTOs in this codebase skip validation isn't 27 separate small mistakes; it's one missing habit, repeated. The fix that actually sticks isn't "go add `@NotBlank` everywhere" (though do that) — it's making the *unvalidated* path the unusual one, e.g. via a code review checklist item or a static check that flags any `@RequestBody` parameter missing `@Valid`.

**On "will this cascade delete ever actually run?"** — the DB-NEW-01 / CQ-10 investigations in this document are both examples of the same skill: a finding's *severity* depends on reachability, not just on what the code says in isolation. "This is a bug" and "this is a bug that is currently executing in production" are different claims requiring different evidence — the first requires reading the code, the second requires tracing actual call sites. Both matter (a currently-dormant landmine is still worth defusing), but conflating them either over- or under-states urgency, and a reader triaging a long findings list needs that distinction to sequence work correctly. When you write up a finding, always ask "have I verified this executes, or am I inferring it from the code's shape?" and say which one you did.

**On atomic operations vs. read-modify-write.** Every "vote count is sometimes wrong" bug in this codebase traces to the same shape: load a value, change it in application memory, write it back. This is safe under exactly one condition — that nothing else can read or write that value between your read and your write — which is never true for a value any concurrent user can touch. The fix is never "add a lock" (locks in a distributed, horizontally-scaled system are their own hard problem); it's "push the increment into the datastore's native atomic operation" (`$inc` in MongoDB, `HINCRBY` in Redis, `UPDATE ... SET x = x + 1` in SQL). If your datastore has an atomic primitive for the operation you're doing, and you're not using it, that's the bug, independent of whether you've hit it in testing yet.

**On why "it ran fine in my testing" doesn't mean "it's not a race condition."** Race conditions are, definitionally, timing-dependent — they don't show up unless two operations actually interleave, which single-user manual testing essentially never produces. This is why REL-01/REL-06/REL-NEW-01 all existed un-caught in a codebase with 57 test files: none of those tests spawn concurrent requests against the same resource. If a piece of code's correctness depends on operations *not* interleaving, you need a test that deliberately makes them interleave (an `ExecutorService` with N threads hitting the same endpoint/service method), not just a test that exercises the code once.

**On why static review has a ceiling, and this project is a good example of it.** The hate-api Dockerfile/`requirements.txt` mismatch (§11.1) is the clearest illustration in this whole audit: reading `requirements.txt` looks correct, reading the Dockerfile in isolation looks plausible, and it takes actually building the image and watching it crash-loop to discover that the two files disagree about what should be installed. A sufficiently careful static reviewer *could* have cross-referenced the two files and noticed the Dockerfile never references `requirements.txt` by name — but this is exactly the kind of cross-file consistency check that's easy to skip when reading one file at a time, and is only forced into visibility by execution. The general principle: for anything with a build step (Docker images, compiled artifacts, generated config), treat "I read the source and it looks right" and "I built/ran it and confirmed it behaves right" as two different, both-necessary levels of verification — and prefer catching this class of bug in CI (§11.3's proposed smoke-import test) over relying on someone reading carefully enough to catch it by eye.

**On distinguishing a peer review's claims from ground truth.** §15.3 of this document is the most important methodological note here: two independent, competent reviews looked at the same six lines of `Movie.java`/`MovieReviewService.java` and reached opposite conclusions about whether a bug existed. The way to resolve that isn't to average the two opinions or defer to whichever one sounds more confident — it's to trace the actual runtime values by hand (what does `ratingCount` equal, concretely, at the moment the guard clause runs?) until the disagreement resolves itself against evidence rather than assertion. Any time you're synthesizing multiple reviews (human or automated) of the same code and they disagree, that disagreement is a *pointer to where to look closer*, not something to smooth over by picking a side.
