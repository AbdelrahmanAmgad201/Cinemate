# Cinemate — Complete Engineering Audit


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

It is **not yet production-ready**, though the gap has narrowed since this document was first written — the Quick Wins and Medium-Term Improvements from §17 are done (see [`DONE.md`](DONE.md)): input validation, atomic counters, entity-leak fixes, and the watch-party origin/partyId hardening all landed. What's left clusters around three themes:

1. **STOMP-level auth on the watch-party WebSocket is still missing** (REL-08) — origin is now restricted and `partyId` is server-generated only, but a connected client still isn't authenticated at the message level. Verification codes are also still 6-digit integers with no rate limiting (SEC-03).
2. **Operational maturity is early-stage.** No metrics, no structured logging, no request tracing, one CI workflow (backend tests only), no dependency scanning, no migration tool (schema evolution still relies on Hibernate `ddl-auto`, now at least consistent between dev/prod), no backup strategy for either database.
3. **The watch-party microservice extraction still carries more operational cost than benefit** (ARC-02/ARCH-NEW-02) — network hop, second attack surface, startup coupling, no independent scaling realized yet.

None of this is surprising or shameful for a project at this maturity level. The purpose of this document is to be a concrete, prioritized list of what's left to fix, why, and in what order.

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
- **The internal API key filter concept** (`InternalApiKeyFilter`) is structurally the right idea for service-to-service auth; its fail-open default has since been fixed (formerly SEC-08, see [`DONE.md`](DONE.md)).
- **Testcontainers usage** (`AbstractMongoIntegrationTest`) shows real awareness of proper integration-test infrastructure, not just unit mocks.
- **Batch processing in `CascadeDeletionService`** uses a `BATCH_SIZE` constant and processes deletions in chunks rather than loading everything at once.

### Weaknesses

- **The watch-party microservice extraction still has more operational cost than realized benefit** (network hop, auth surface, startup coupling; no independent scaling, no independent data store of its own).
- **Zero production observability**: no metrics, no tracing, no structured logs, no alerting.
- **CI covers one of four services.**
- **REL-08 (no STOMP-level auth on the watch-party WebSocket) and SEC-03 (enumerable verification codes) remain the two most serious open findings** — everything else input-validation- and concurrency-related from the original review has been fixed (see [`DONE.md`](DONE.md)).

---

## 3. Findings Index & Severity Distribution

### Severity distribution

> **Status note:** this table reflects only the **remaining open findings**. Everything else — the original CQ/SEC batch, all Quick Wins, all Medium-Term Improvements, the Caffeine→Redis cache migration (ARC-06/PERF-03), and this pass's batch of simple no-architecture-change/no-new-dependency fixes (CQ-NEW-04/05, SEC-NEW-03/04/05/06, PERF-01/04/07/08/NEW-01, REL-09, REL-NEW-02, DB-NEW-01/04, §11.4/11.6, FE-02/03/04/06/07/09, FE-NEW-01/03/04, HS-03/07, HS-05's 5b/5c/5d) — has been moved to [`DONE.md`](DONE.md).

| Severity | Count |
|---|---|
| 🔴 Critical | 3 |
| 🟠 High | 10 |
| 🟡 Medium | 18 |
| 🟢 Low | 2 |
| **Total** | **33** |

### Full findings index

| ID | Title | Severity | Section |
|---|---|---|---|
| ARC-01 | Unjustified dual-database split (MySQL + MongoDB) | High | §4 |
| ARC-02 | Watch-party microservice extraction is premature | Medium | §4 |
| ARC-03 | Missing API gateway / reverse proxy layer | Medium | §4 |
| ARC-04 | Verification codes stored in MySQL (timing attack vector) | High | §4 |
| ARC-05 | Three-role authentication in one service (flat role model) | Medium | §4 |
| ARC-08 | No observability infrastructure | Medium | §4 |
| ARCH-NEW-02 | Watch-party extraction cost is even higher than measured | Medium | §4 |
| CQ-11 | `HateSpeechScheduler` re-analyzes all comments from last 24h | Medium | §5 |
| API-NEW-02 | REST convention violations: read-only endpoints as `POST` | Medium | §6 |
| SEC-03 | Verification code enumerable (900k combos, no rate limit) | Critical | §7 |
| PERF-06 | `AccessService.canDeleteComment()` issues up to 3 sequential queries | Medium | §8 |
| PERF-09 | `SimpleClientHttpRequestFactory` has no connection pooling | Medium | §8 |
| PERF-NEW-02 | Vote read-modify-write independently rediscovered (≈REL-01) | — | §8 |
| REL-08 | WebSocket control channel has no authentication | Critical | §9 |
| DB-NEW-02 | `Vote.isPost` should be an enum (index-name half already fixed) | Medium | §10 |
| DB-NEW-03 | Inconsistent soft-delete across MongoDB collections | Medium | §10 |
| §11.3 | CI covers exactly one of four services | Medium | §11 |
| §11.5 | No backup/restore strategy for either database | Medium | §11 |
| FE-01 | JWT stored in `sessionStorage` (XSS accessible) | High | §12 |
| FE-05 | Duplicate API files (forum/forums, post/posts) | Medium | §12 |
| FE-08 | No frontend tests | High | §12 |
| FE-NEW-02 | `PostComments.jsx` is 765 lines doing five unrelated jobs | Low | §12 |
| HS-01 | Synchronous moderation blocks every write operation | Critical | §13 |
| HS-04 | Comments not moderated at write time (inconsistency) | High | §13 |
| HS-05 | hate-api `app.py` is production-unready (5e/5f remain of 7 sub-findings) | High | §13 |
| HS-06 | Nightly scheduler is an O(N) sequential HTTP bomb | High | §13 |
| HS-08 | Incomplete JSON escaping in HTTP body construction — **fixed** (same defect as former CQ-01) | Medium | §13 |
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

#### ARCH-NEW-02 — `watch-party` extraction cost is even higher than the original audit measured
**Severity:** Medium (confirms/extends ARC-02) | **Effort:** Medium

The existing ARC-02 finding is correct that the microservice is synchronously coupled and has a hard startup dependency. Independent review of the full `watch-party` module adds detail that strengthens the case for folding it back into the monolith (or re-architecting the boundary):

- The STOMP `/ws` control channel still has **no message-level authentication** (REL-08) — the origin-wildcard and client-suppliable-`partyId` parts of this same surface were fixed (formerly SEC-NEW-02, see [`DONE.md`](DONE.md)), but a connected client isn't authenticated.
- It has **essentially zero test coverage** (one placeholder context-load test for a module with real concurrency and security surface — see TEST-04).

None of this is a reason the service *shouldn't* exist — real-time fan-out genuinely benefits from Redis pub/sub — but as currently built it is a second attack surface and a second source of truth with weaker guarantees than the monolith it was split from, for no scaling benefit realized yet. **Recommendation stands as ARC-02 Option A** (fold back into the monolith as an internal package, keep Redis for pub/sub only) **unless there is a concrete near-term plan to scale it independently** — in which case, fix REL-08 and SEC-NEW-03 first.

---

## 5. Code Quality Review

The backend is a Spring Boot monolith with ~24 packages. Overall structure is clean, but there are recurring patterns of poor exception handling, duplicated utility code, mixed injection styles, and a few critical logic bugs.

> **Status note:** CQ-01 through CQ-10 and CQ-12 (11 findings) have been fixed and moved to [`DONE.md`](DONE.md). CQ-11 remains open below — it needs a real architectural change (an `isModerated` flag plus paginated/parallel processing), not a small fix.

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

## 6. API Design Review

*(New top-level section — the original audit touched API design tangentially through SEC-06's missing leading slash and FE-09's wasted response data, but never assessed it holistically. Independent review found this deserves to be a first-class category.)*

#### API-NEW-02 — REST convention violations: read-only endpoints exposed as `POST`
**Severity:** Medium | **Category:** API Design | **Files:** `AdminController.java` (`/v1/find-admin-requests`, `/v1/get-pending-requests`), `MovieController.java` (`/v1/get-specific-movie-overview`), `OrganizationController.java` (`/v1/get-organization-movies`) — 12+ occurrences

Read-only lookups implemented as `@PostMapping`. Beyond the RFC 7231 semantic violation, this has a concrete cost: these responses are never cache-eligible at any layer (browser, CDN, reverse proxy) because caches don't cache POST by default, and it makes the API surface unpredictable for API consumers/tooling (OpenAPI generators, Postman collections, integration testers all assume GET-for-reads). This is inconsistent with the fact that `/api/movie/**` search *does* correctly differentiate (though it's also `POST` for search, which is defensible for a complex filter body — the admin/org endpoints above have no such excuse, they take no meaningful body).

**Recommended fix:** convert to `@GetMapping` with `@RequestParam`/`@PathVariable` where the request has no meaningful body; keep `POST` only where the request genuinely represents a complex query body that doesn't fit in a query string.

---

## 7. Security Review

> **Status note:** SEC-01, SEC-02, SEC-04 through SEC-10 (9 of the 10 original-audit security findings) have been fixed and moved to [`DONE.md`](DONE.md), including the real production-blocking Lombok/`@Lazy` bug that verifying these fixes surfaced. SEC-03 remains open below by explicit decision — the plan is to handle it at a reverse-proxy/rate-limiter layer rather than in-process.

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

## 8. Performance Review

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

#### PERF-NEW-02 — Vote/comment read-modify-write pattern independently rediscovered with the identical atomic fix
This is the same underlying defect as REL-01 (§9). Both this independent review and the original audit arrived at the identical root cause and fix (`mongoTemplate.updateFirst(..., new Update().inc(...))`) with no cross-contamination between the two reviews. Treat this as strong corroboration rather than a separate new finding — see §15.1.

---

## 9. Reliability Review

### REL-08 — WebSocket Control Channel Has No Authentication

**Severity:** Critical | **Effort to fix:** Medium

**Status:** partially fixed. The origin restriction and client-suppliable-`partyId` parts of this finding were fixed under SEC-NEW-02 (see [`DONE.md`](DONE.md)) — `setAllowedOriginPatterns("*")` is gone, `partyId` is always server-generated now. **Message-level authentication is still missing**, which is what this finding is actually about.

**Description.** The WebSocket STOMP endpoint `/ws` in the watch-party service has no authentication at the message level. Any client that knows a `partyId` can send `PLAY`, `PAUSE`, `SEEK` events to all party members.

**Evidence.**
```java
// WebSocketController.java line 22
@MessageMapping("/party/{partyId}/control")
public void handleControl(@DestinationVariable String partyId, @Payload PartyEvent event) {
    // no authentication check — accepts any message
    redisPublisher.publish(partyId, event);
}
```

**Impact.** Any anonymous client that reaches the (now origin-restricted) WebSocket can still disrupt any watch party by sending pause/seek events for a `partyId` it knows or scrapes. Chat messages can be sent by unauthenticated users with any `userName` — see SEC-NEW-05 for the resulting XSS delivery path.

**Recommended Fix.**
1. Validate a JWT in the STOMP `CONNECT` frame using a `ChannelInterceptor`.
2. Extract `userId` from the JWT and compare it against the party's member set before broadcasting.

---

## 10. Database Review

*(New top-level section. ARC-01 in §4 (no cross-store transactions), ARC-07 (ID conversion hack), and PERF-08 (missing movie indexes) cover the most important architectural database issues already in the original audit. Independent schema-level review adds the following, with severities recalibrated against actual reachability — see the note on DB-NEW-01, the clearest example in this document of the kind of verification this whole review tries to model.)*

#### DB-NEW-02 — `Vote.isPost` should be an enum
**Severity:** Medium | **Category:** Schema Design | **File:** `backend/src/main/java/org/example/backend/vote/Vote.java:22,33-34`

The compound-index half of this finding is fixed (both indexes referenced a `targetType` field that doesn't exist — the actual discriminator is `isPost`; both now correctly reference `isPost`, see [`DONE.md`](DONE.md)). What's still open: a `Boolean` discriminator for "is this a vote on a post or a comment" doesn't extend (what happens when forum-level voting is added?) and reads worse at every call site than `VoteTargetType.POST`/`.COMMENT` would. **Recommended fix:** convert to an enum — larger change than the index fix, touches every call site plus existing data, deliberately left for a separate pass.

#### DB-NEW-03 — Inconsistent soft-delete: some MongoDB collections have no `deletedAt`, and at least one repository method hard-deletes despite the collection having an `isDeleted` field
**Severity:** Medium | **Category:** Data Integrity / Auditability

Most soft-deletable documents (`Post`, `Comment`) have `isDeleted` but not all have a `deletedAt` timestamp, meaning "when was this deleted" is unanswerable for compliance/audit purposes on some collections while answerable on others. Separately, `FollowingRepository` exposes a `deleteByUserIdAndForumId()` method that performs a genuine hard delete on a collection whose sibling entity elsewhere in the same domain uses soft-delete — an easy trap for the next engineer who assumes "this codebase soft-deletes" is a universal rule (it's the dominant pattern, but not a guaranteed one). **Recommended fix:** introduce a `@MappedSuperclass`/base document with `isDeleted` + `deletedAt` applied uniformly, and audit every repository for hard-delete methods that should be soft-delete instead.

---

## 11. DevOps & Infrastructure Review

*(The original audit's coverage here was limited to ARC-08/TEST-05 — observability only. This section is substantially expanded with findings that mostly required actually running the stack, not just reading it.)*

### 11.3 CI covers exactly one of four services
**Severity:** Medium | **Category:** CI/CD

`.github/workflows/backend-tests.yml` is the **only** workflow in the repository (verified: `.github/workflows` contains one file, no `dependabot.yml`, no other workflow). `watch-party`, `hate-api`, and `frontend` have zero CI — no build verification, no test execution (frontend has no tests to run in any case, per FE-08), no lint step, no Docker image build check. A change that breaks the `watch-party` build or introduces a `hate-api` regression (like §11.1) will not be caught until someone runs `docker compose up` locally.

**Recommended fix (quick win):** add `watch-party-tests.yml` (mirror the backend workflow), `hate-api-lint.yml` (at minimum `pip install -r requirements.txt && python -c "import app"` as a smoke import test, which alone would have caught §11.1 immediately), and `frontend-lint.yml` (`npm ci && npm run lint`, plus `npm run build` as a build-integrity check even before tests exist). Add Dependabot for at least the three `pom.xml`/`package.json`/`requirements.txt` ecosystems — zero-config, catches exactly the kind of drift in §11.1.

### 11.5 No backup/restore strategy for either database
**Severity:** Medium | **Category:** Reliability

`mysql-data` and `mongo-data` are plain named Docker volumes with no backup automation, no documented restore procedure, and no snapshot schedule anywhere in the repo or docs. For a project that stores user accounts, posts, and reviews, this means **any data-loss event (bad migration, disk failure, accidental `docker volume rm`) is unrecoverable**. This doesn't need to be sophisticated — even a documented `mysqldump`/`mongodump` cron container added to `compose.yaml` writing to a bind-mounted host directory would close the most acute version of this gap.

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

### FE-05 — Duplicate API Files (`forum-api.js` and `forums-api.js`, `post-api.js` and `posts-api.js`)

**Severity:** Medium | **Effort to fix:** Low

**Description.** There are two API files for forums (`forum-api.js` and `forums-api.js`) and two for posts (`post-api.js` and `posts-api.js`). This indicates fragmented, duplicated API layer modules. *(Filenames updated from `.jsx` to `.js` — see the now-fixed FE-04 in [`DONE.md`](DONE.md) — the duplication itself is unrelated and still open.)*

**Evidence.**
```
frontend/src/api/forum-api.js
frontend/src/api/forums-api.js
frontend/src/api/post-api.js
frontend/src/api/posts-api.js
```

**Impact.** Different components import from different files, making it unclear which is canonical. Updates to API endpoints must be made in multiple places.

**Recommended Fix.** Consolidate into a single `forum-api.js` and `post-api.js`. Review imports across the project to update references.

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

### New findings from the independent review

#### FE-NEW-02 — `PostComments.jsx` is 765 lines doing five unrelated jobs
**Severity:** Low | **Category:** Maintainability

One component handles user-name-cache fetching, comment CRUD, nested reply rendering, vote handling, and menu state — roughly 5-10x the size of a well-scoped component. This isn't a style nitpick; a component this size is the one that will accumulate the next ten bug reports because no single change to it can be reasoned about in isolation. Recommended: extract `useUserNameCache`, `useCommentVoting`, and a `CommentItem` component per the existing `*View`/hook conventions already used elsewhere in the codebase.

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
3. **Still slow regardless of transaction boundaries:** the no-op `@Transactional` that used to wrap this call has since been removed (formerly HS-02/PERF-02, see [`DONE.md`](DONE.md)), but the synchronous inference call itself is still on the request's critical path — removing the transaction stopped it from holding a MySQL connection hostage, it didn't make posting faster. *(See PERF-NEW-01 for the separate, still-open connection-pool-sizing question.)*
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

**5a. `print(request.text)` Logs Every User Message to Stdout — FIXED**
```python
# app.py line 43 (former state)
@api_router.post("/analyze")
def analyze(request: TextRequest):
    print(request.text)   # ← logs every analyzed string
    return analyze_text(request.text)
```
This was a debug statement that was never removed, logging every post, comment, and forum name to stdout verbatim in production — a privacy leak, since server access logs would then contain user-generated content in unstructured form alongside framework logs. *(Same defect as former SEC-07; both were fixed together — see [`DONE.md`](DONE.md). The `/analyze` endpoint also gained an API-key check as part of that fix, which sub-item 5b below did not cover.)*

**5b/5c/5d — FIXED.** Redundant `nltk.download()` calls at module load removed (NLTK data is already pre-baked into the image at `/nltk_data`, see the Dockerfile); a one-time warmup inference now runs at startup so the first real request isn't slowed by PyTorch's lazy JIT caching; `TextRequest.text` now has `max_length=10_000` (`Field(..., max_length=MAX_TEXT_LENGTH)`), closing the unbounded-input DoS vector. See [`DONE.md`](DONE.md).

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

**5g. No API Authentication — FIXED**
```python
# Former state: no dependency, no header check, no API key validation
@api_router.post("/analyze")
def analyze(request: TextRequest):
    ...
```
Previously, any request that reached the service was processed, and the Docker Compose file exposed port 8000 on the host, making it publicly reachable. *(Same underlying issue as former SEC-07 — this was the application-layer half of it, SEC-07 was the network-layer half; both fixed together, see [`DONE.md`](DONE.md): the host port is gone and `/analyze` now requires an `X-Internal-API-Key` header.)*

**Remaining work on `app.py`:** 5a/5b/5c/5d/5g are fixed (see [`DONE.md`](DONE.md)). What's left is 5e (single-worker throughput ceiling — needs an infra/architecture decision: more workers, an async+semaphore rewrite, or GPU) and 5f (the implicit-safe-on-empty-string behavior, which is correct but worth making explicit if this file is touched again).

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

---

### HS-08 — Incomplete JSON Escaping in HTTP Body Construction — **FIXED**

**Severity:** Medium | **Effort to fix:** Low

> **Status: fixed.** This was the identical underlying defect as the former CQ-01 (see [`DONE.md`](DONE.md)), fixed in the same change: `HateSpeechService.analyzeText()` no longer builds JSON by hand — it now passes a `Map<String, String>` body and lets Spring's Jackson message converter serialize it, which escapes newlines, control characters, and everything else correctly. The description below is preserved as the original evidence/impact record.

**Description.** `HateSpeechService.analyzeText()` built the JSON request body by hand:
```java
// HateSpeechService.java line 34 (prior to fix)
String body = "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
```
The escaping handled `\` and `"`, but missed: newlines (`\n` → must become `\\n` in JSON strings), carriage returns (`\r`), tabs (`\t`), null bytes (`\0`), and other control characters (U+0000–U+001F).

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

**Description.** The test suite has no tests covering: JWT token validation edge cases (expired token, tampered signature, missing claims); the `InternalApiKeyFilter` (watch-party auth filter) behavior when key is missing vs. present vs. wrong; the hate-speech moderation path being bypassed when the service is unavailable; concurrent vote/comment operations (the atomic `$inc` fix that closed the former REL-01 race — see [`DONE.md`](DONE.md) — has no regression test guarding it).

**Evidence.** `SecurityIntegrationTest.java` exists but is the only security integration test. `HateSpeechServiceTest.java` exists but likely only unit-tests the service call, not the fail-open behavior. `WatchPartyApplicationTests.java` in the watch-party module is a placeholder (Spring Boot test that only checks context loads).

**Impact.** Critical security behaviors are untested and can regress silently.

**Recommended Fix.** Add: `JWTProviderTest` (test expired token rejection, tampered token rejection, missing `role` claim); `InternalApiKeyFilterTest` (test 401 on missing/wrong key, 200 on correct key, health bypass — including the `startsWith` bug in SEC-NEW-04); `HateSpeechIntegrationTest` (test that a `RestClientException` from hate-api results in fail-open); `VoteConcurrencyTest` (use `ExecutorService` with 10 threads to verify vote counts stay correct after concurrent voting — regression coverage for the atomic-counter fix, not a bug hunt).

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

**Impact.** `WatchPartyService` (346 lines) including `createParty`, `joinParty`, `leaveParty`, and `deletePartyInternal` are entirely untested. The N+1 Redis read in `getPartyMembers` (PERF-01, still open) is invisible without tests — and the atomic-join fix that closed REL-06/REL-NEW-01 (see [`DONE.md`](DONE.md)) has no regression test guarding it either, so it could silently regress back to a race condition.

**Recommended Fix.** Use Testcontainers with `redis:7` to spin up a real Redis instance for the tests. Test: party creation and retrieval; join/leave with duplicate prevention; party auto-deletion when participant count reaches 0; TTL expiry behavior (use a short test TTL); and — given REL-NEW-01's precise race trace — a concurrent-join test using an `ExecutorService` with multiple threads hitting `joinParty` for the same user simultaneously.

---

### TEST-05 — No Observability / Metrics

**Severity:** High | **Effort to fix:** Medium

**Description.** The application has no metrics, no distributed tracing, and no health indicators beyond a basic HTTP ping.

**Evidence.** `pom.xml`: no `spring-boot-starter-actuator` dependency (independently re-confirmed by reading `pom.xml` directly). `HealthController.java` returns a static `{ "status": "UP" }` — no DB connectivity check, no Redis connectivity check, no hate-api reachability check. No Micrometer, Prometheus, or OpenTelemetry dependency anywhere in either service.

**Impact.** In production, there is no way to know: how many requests per second each endpoint handles; what the hate-api call latency/failure rate is; whether Redis is connected and healthy; JVM heap usage or GC pressure. Incidents can only be detected by users reporting errors, not proactively by alerts. *(`GlobalExceptionHandler` now logs every 500 — formerly CQ-NEW-01, see [`DONE.md`](DONE.md) — but that's still just a log line on a host nobody's watching without the metrics/alerting infrastructure this finding is about.)*

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

**CQ-10 (`Movie.ratingCount` initialized to 1 instead of 0) — the original audit says this is a real bug; a parallel automated reviewer working independently during the follow-up claimed it was "not confirmed" and that the logic in `MovieReviewService` "increments properly."** The actual data flow was traced by hand to adjudicate: `Movie.onCreate()` (`@PrePersist`) sets `ratingCount = 1` (not `0`) whenever it's null — which for a freshly created movie means it becomes non-null-but-wrong *before* any review exists. `MovieReviewService.addOrUpdateReview()` then has a guard, `if (movie.getRatingCount() == null) movie.setRatingCount(0)`, which the contradicting claim read as protecting against exactly this — but the guard never fires, because by the time a review is added, `ratingCount` is already `1`, not `null`. So the first real review does `ratingCount = 1 + 1 = 2`, and `averageRating = ratingSum / 2` instead of `/ 1` — precisely the `5-star review shows as 2.5` bug the original audit describes. **Verdict: the original audit (CQ-10) is correct; the contradicting claim was wrong**, and the error came from checking the null-guard in isolation without tracing what value the field actually holds by the time that guard runs. This is now the single most rigorously verified finding in this document, and it's a fairly severe one — it's a core-feature (movie rating) correctness bug, not an edge case. *(Status: fixed — `ratingCount` now defaults to `0`; see [`DONE.md`](DONE.md). The dispute-resolution story above is kept in full because the verification methodology it demonstrates outlives the bug itself.)*

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
8. **🟠 Remove hate-api Host Port Exposure and Debug `print()` (SEC-07) — done.** `ports: "8000:8000"` removed from compose (now internal-only via `expose`); `print(request.text)` removed from `app.py`; an `X-Internal-API-Key` check was added as well. *Effort: Low | Impact: Privacy + closes direct external access to the ML model.*
9. **🟠 Fix `InternalApiKeyFilter` to Fail-Closed (SEC-08) — done.** Now rejects with 503 when `WATCHPARTY_KEY` is not configured, instead of allowing all requests. *Effort: Low | Impact: Prevents undetected misconfiguration from opening the internal API.*
10. **🟠 Move HTTP Calls Outside `@Transactional` Boundaries (PERF-02).** All `callMicroservice*()` methods are inside `@Transactional` blocks, holding DB connections for up to 10 s. *Effort: Low | Impact: Prevents connection pool exhaustion under moderate traffic.*

### 16.2 Files With the Most Issues

| File | Issues |
|---|---|
| `backend WatchPartyService.java` | REL-02, PERF-02, ARC-02 |
| `VerificationService.java` | REL-04, REL-05 *(CQ-02, SEC-10 fixed)* |
| `CascadeDeletionService.java` | REL-07, TEST-03, CQ-NEW-05 |
| `VoteService.java` | REL-01 *(CQ-06 fixed)* |
| `CommentService.java` | REL-01, REL-03 |
| `HateSpeechScheduler.java` | CQ-11, PERF-04, HS-06 |
| `watch-party WatchPartyService.java` | REL-06, PERF-01, REL-NEW-01, SEC-NEW-02 |
| `frontend/.env.production` | FE-02 |
| `Movie.java` *(added by follow-up)* | CQ-NEW-02, PERF-08 *(CQ-10 fixed)* |
| `GlobalExceptionHandler.java` *(added by follow-up)* | CQ-NEW-01, API-NEW-03 |
| `hate-api/Dockerfile` *(added by follow-up)* | SEC-NEW-06, §11.1 |

### 16.3 Positive Observations (from the original audit, all independently reconfirmed — see §15.5)

1. **Docker Compose health checks** — all services have proper health checks with appropriate timeouts.
2. **Soft-delete pattern** — consistent across MongoDB collections with async cascade deletion.
3. **Internal API key filter** — the `InternalApiKeyFilter` concept is correct; its fail-open default has since been fixed ([`DONE.md`](DONE.md)).
4. **MongoDB compound indexes** — `Comment` has well-designed compound indexes covering common query patterns.
5. **Hate-speech fail-open design** — explicitly documented and intentional, preventing content blocking during outages.
6. **Separation of `AccessService`** — centralizing authorization logic in a dedicated service is good architecture.
7. **Testcontainers usage** — `AbstractMongoIntegrationTest` shows awareness of proper integration test infrastructure.
8. **Batch processing in `CascadeDeletionService`** — uses a `BATCH_SIZE` constant and processes in chunks, not all at once.
9. **Multi-stage Dockerfiles** — all three services use multi-stage builds correctly.
10. **CORS configuration via environment variables** — the `CORS_ALLOWED_ORIGINS` env-var pattern is clean and deployment-friendly.

---

## 17. Prioritized Action Plan


### Medium-Term Improvements (1-2 sprints)

**All items done — see [`DONE.md`](DONE.md).** Covered: Bean Validation + `@Valid` everywhere (ARCH-NEW-01/SEC-NEW-01), pagination on every list endpoint with a max-size cap (API-NEW-01), entities no longer returned directly from controllers (CQ-NEW-02/CQ-NEW-03), manual try/catch removed from controllers (API-NEW-03), plus the remaining Sprint-1/2 carryovers (`REL-03`, `REL-05`, `PERF-05`, `ARC-07`).

One item raised during this pass but deliberately left open, still requiring a decision: `AdminController`/`OrganizationController` still return the raw `Requests` entity on a few endpoints — same class of issue as CQ-NEW-03, flagged separately rather than folded in silently.

### Long-Term Refactoring Roadmap (quarter-scale)

1. Resolve the watch-party architectural question explicitly (ARC-02/ARCH-NEW-02): either fold it back into the monolith, or commit to it as a real independently-scaled service and fix its security/concurrency posture to match that responsibility — but fix REL-08/SEC-NEW-03 regardless of which direction is chosen.
2. Evaluate consolidating MySQL + MongoDB, or at minimum introduce a saga/outbox pattern for the operations that currently span both with no compensation (ARC-01).
3. Consolidate the three-role authentication model (`User`/`Admin`/`Organization` as separate tables) into a single discriminated `accounts` model with a real permission hierarchy (ARC-05).
4. Longer-term, decouple the ID systems entirely rather than encoding one inside the other — store the MySQL `userId` as a plain field on MongoDB documents instead of converting it into a fake `ObjectId` (extends the now-fixed ARC-07, see [`DONE.md`](DONE.md)).
5. Add end-to-end and load testing for the Redis-backed cache and JWT/OAuth-code stores (ARC-06, now fixed — see [`DONE.md`](DONE.md)) to actually verify the horizontal-scaling assumption they were built for: confirm two backend replicas genuinely share cache entries, a revoked token is rejected on every replica, and an OAuth exchange code issued by one replica is redeemable on another.
6. Build out a real observability stack (Prometheus + Grafana + log aggregation) rather than the actuator/metrics-endpoint baseline from the Medium-Term list — the baseline gets you data, this gets you the ability to actually act on it (dashboards, alerting rules, on-call runbooks).
7. Add end-to-end and load testing for the concurrency-sensitive paths (voting, watch-party join/leave) specifically — the atomic-operation fixes already landed (see [`DONE.md`](DONE.md)) but have no regression protection, so they could silently regress back to read-modify-write the next time someone "simplifies" that code.
8. Replace `@SpringBootTest`+manual-mock anti-pattern (TEST-01) codebase-wide with `@ExtendWith(MockitoExtension.class)`; add the security-path tests from TEST-02; add watch-party unit/integration tests (TEST-04); migrate frontend JWT storage from `sessionStorage` to an `HttpOnly` cookie (FE-01); add a Vitest + React Testing Library suite for the frontend (FE-08).

---

## 18. Learning Notes

These are the underlying engineering principles behind the recommendations above, written out explicitly so the patterns generalize past this specific codebase.

**On input validation.** Validation isn't a UX feature ("show a nice error message") — it's a trust-boundary control. Every place external input crosses into your system (an HTTP request body, a WebSocket message, a query parameter) is a place where you either enforce your invariants or inherit whatever invariants the caller happened to have (none, usually). The fact that 27 of 28 DTOs in this codebase skip validation isn't 27 separate small mistakes; it's one missing habit, repeated. The fix that actually sticks isn't "go add `@NotBlank` everywhere" (though do that) — it's making the *unvalidated* path the unusual one, e.g. via a code review checklist item or a static check that flags any `@RequestBody` parameter missing `@Valid`.

**On "will this cascade delete ever actually run?"** — the DB-NEW-01 / CQ-10 investigations in this document are both examples of the same skill: a finding's *severity* depends on reachability, not just on what the code says in isolation. "This is a bug" and "this is a bug that is currently executing in production" are different claims requiring different evidence — the first requires reading the code, the second requires tracing actual call sites. Both matter (a currently-dormant landmine is still worth defusing), but conflating them either over- or under-states urgency, and a reader triaging a long findings list needs that distinction to sequence work correctly. When you write up a finding, always ask "have I verified this executes, or am I inferring it from the code's shape?" and say which one you did.

**On atomic operations vs. read-modify-write.** Every "vote count is sometimes wrong" bug in this codebase traces to the same shape: load a value, change it in application memory, write it back. This is safe under exactly one condition — that nothing else can read or write that value between your read and your write — which is never true for a value any concurrent user can touch. The fix is never "add a lock" (locks in a distributed, horizontally-scaled system are their own hard problem); it's "push the increment into the datastore's native atomic operation" (`$inc` in MongoDB, `HINCRBY` in Redis, `UPDATE ... SET x = x + 1` in SQL). If your datastore has an atomic primitive for the operation you're doing, and you're not using it, that's the bug, independent of whether you've hit it in testing yet.
