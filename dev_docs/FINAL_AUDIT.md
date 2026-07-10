# Cinemate — Open Issues & Lessons Learned

**Status:** Post-implementation audit. Extensive fixes have landed (see `DONE.md`). This document lists only remaining open issues and key architectural lessons.

---

## Open Critical Issues


### HS-01 — Synchronous Moderation Blocks Every Write Operation
**Severity:** Critical | **Effort:** Medium

Posts/forums block on hate-api inference (200–2000ms CPU, 10s timeout). Comments skip write-time moderation entirely. Single uvicorn worker caps platform to ~2 writes/second. Recommended: decouple into async post-publish moderation with `status=PENDING`, fire-and-forget worker thread, update to `PUBLISHED` on safe result. Combine title+content into single HTTP call.

### FE-01 — JWT Stored in `sessionStorage` (XSS Accessible)
**Severity:** High | **Effort:** Medium

Any XSS vulnerability leads to token theft. Migrate to `HttpOnly` cookies with `Secure; SameSite=Strict`.

---

## Open High-Priority Issues

### ARC-01 — Dual-Database Split (MySQL + MongoDB) With No Transaction Boundary
**Severity:** High | **Effort:** High

MySQL holds relational data (User, Movie, Organization, WatchParty), MongoDB holds social content (Post, Comment, Vote, Forum). WatchParty lives in both with separate write paths and no compensation on failure. If `callMicroserviceInitialize()` throws after MySQL commit, the party exists in MySQL but not in Redis.

**Fix options:** (a) consolidate to single database; (b) add compensating cleanup (rollback MySQL on microservice failure); (c) introduce saga/outbox pattern for cross-store operations.

### ARC-02 / ARCH-NEW-02 — Watch-Party Microservice Extraction Is Premature
**Severity:** Medium | **Effort:** Medium

Service owns no persistent data (ephemeral Redis), has hard startup dependency (`depends_on: condition: service_healthy`), and is called **synchronously** via `RestTemplate`. Contains ~130 lines of HTTP boilerplate and no independent scaling benefit realized yet. Also lacks STOMP-level auth (REL-08) and has near-zero test coverage (TEST-04).

**Recommendation:** Fold back into monolith as internal `WatchPartyWebSocketHandler` package (keeps Redis pub/sub), or make calls async via event queue.

### HS-06 — Nightly Scheduler Is O(N) Sequential HTTP Bomb
**Severity:** High | **Effort:** Medium

Re-scans all comments from last 24 hours, one HTTP call per comment sequentially. At 10,000 comments/day, runtime is 83+ minutes (10s timeout each = 28 hours, exceeding midnight cycle). Also loads entire collection into memory. 

**Fix:** Add `isModerated` flag to `Comment`. Scheduler only processes `isModerated=false`. Paginate. Use thread pool for parallel HTTP.

### ARC-05 — Three-Role Authentication Model (Flat, Hard-Coded)
**Severity:** Medium | **Effort:** Medium

`User`, `Admin`, `Organization` are separate tables/entities with one `AuthenticationService` switching on role string. Frontend must submit role in login request (non-standard, enables brute-force by role). Adding a new role requires changes to auth service, security config, and 3+ verification methods.

**Fix:** Consolidate into single `accounts` table with `@DiscriminatorColumn`, use Spring `GrantedAuthority` hierarchy, derive role from account record (not from request body).

### DB-NEW-03 — Inconsistent Soft-Delete: Some Collections Lack `deletedAt`
**Severity:** Medium | **Effort:** Medium

Most soft-deleted documents have `isDeleted` but not `deletedAt`, making "when was this deleted?" unanswerable for compliance. `FollowingRepository` hard-deletes despite soft-delete elsewhere in the same domain.

**Fix:** Introduce `@MappedSuperclass` base document with `isDeleted + deletedAt`, audit all repositories for hard-delete methods that should be soft-delete.

### TEST-02 — No Tests for Critical Security Paths
**Severity:** High | **Effort:** Medium

Missing coverage: JWT edge cases (expired, tampered, missing claims), `InternalApiKeyFilter` with missing/wrong/present key, hate-api unavailability fail-open, concurrent vote/comment operations (atomic `$inc` fix has no regression test).

**Fix:** Add `JWTProviderTest`, `InternalApiKeyFilterTest`, `HateSpeechIntegrationTest` (test fail-open), `VoteConcurrencyTest` (10-thread concurrent voting).

### ARC-08 — No Observability Infrastructure
**Severity:** Medium | **Effort:** Medium

No metrics, no distributed tracing, no centralized logging. Only per-service SLF4J to stdout. When the system degrades, there is no way to diagnose without SSH.

**Fix:** Add `spring-boot-starter-actuator` + Prometheus endpoint, add `logstash-logback-encoder` for JSON logging, add Prometheus + Grafana to compose.yaml.

---

## Open Medium-Priority Issues

### CQ-11 — `HateSpeechScheduler` Re-Analyzes All Comments From Last 24 Hours
**Severity:** Medium | **Effort:** Medium

Nightly scheduler fetches all 24h comments and re-runs each through hate-api individually, one HTTP call per comment. Duplicated work (comments already moderated at write time, or skipped entirely per HS-04). No batching, no parallelism.

**Fix:** Add `isModerated` flag. Only re-scan unmoderated comments in pages with thread pool.

### PERF-06 — `AccessService.canDeleteComment()` Issues 3 Sequential MongoDB Queries
**Severity:** Medium | **Effort:** Medium

Fetches comment → post → forum to check access. Denormalize `postOwnerId` and `forumId` into `Comment` document.

### TEST-05 — No Observability / Metrics
**Severity:** High | **Effort:** Medium

Application has no metrics collection, no health indicators beyond static "UP". No way to know RPS, hate-api latency, Redis connectivity, or JVM pressure. See ARC-08.

### FE-NEW-02 — `PostComments.jsx` Is 765 Lines Doing Five Unrelated Jobs
**Severity:** Low | **Effort:** Medium

Handles user-name cache, comment CRUD, nested reply rendering, vote handling, menu state. Extract `useUserNameCache`, `useCommentVoting`, `CommentItem` component.

### TEST-01 — `@SpringBootTest` + `@BeforeEach` Mock Override Anti-Pattern
**Severity:** High | **Effort:** Medium

Full Spring context loads on every test (slow, requires DB), then autowired beans are immediately replaced with mocks (defeating the purpose). 7 of 57 backend tests do this.

**Fix:** Replace with `@ExtendWith(MockitoExtension.class)`, use `@Mock` and `@InjectMocks`.

### TEST-03 — Only One True End-to-End Integration Test
**Severity:** Medium | **Effort:** High

`ForumPostCommentIntegrationTest` is the only integration test. Cascade deletion (441 lines) has no integration test. `@Async` methods are hard to verify without async integration tests.

**Fix:** Add Testcontainers-based integration tests for cascade deletion, `@Async` methods (using `CompletableFuture` with timeout), watch-party lifecycle.

### HS-05e — Single-Worker Inference Caps Throughput at ~2 Writes/Second
**Severity:** High | **Effort:** Medium

`hate-api` Dockerfile: `CMD ["uvicorn", "app:app", "--workers", "1"]`. PyTorch inference isn't thread-safe; one worker is technically correct for safety but creates a global throughput ceiling regardless of backend scaling.

**Recommendation:** Commit to one of: (a) multiple workers with model-per-worker (4× RAM); (b) async endpoint with semaphore lock (serialize inference, allow concurrent I/O); (c) GPU (drops CPU inference 200–500ms to <20ms).

---

## Architectural Lessons Learned

### 1. Dual-Database Splits Without Transaction Boundaries Cause Silent Data Loss
**Lesson:** The MySQL + MongoDB split in Cinemate (relational data in MySQL, social content in MongoDB, WatchParty spanning both) has no cross-store atomicity. If any microservice HTTP call fails after a local commit, the data is orphaned. This is not a "nice to have" in distributed systems — it's a correctness requirement that must be decided upfront. Choose: (a) consolidate to one database, (b) saga/outbox pattern with compensating transactions, or (c) accept the data-loss risk.

### 2. Premature Microservice Extraction Increases Complexity Without Scaling
**Lesson:** The watch-party service was extracted to "enable scaling," but it owns no persistent data, has a hard startup dependency, and is called synchronously. The result: more operational complexity (two services instead of one), a second attack surface (WebSocket auth, Redis keys, inter-service communication), and zero realized scaling benefit. The decision to extract should have been deferred until one of these was true: (a) the service had its own data store, (b) it was genuinely called asynchronously, or (c) there was a concrete scaling bottleneck that required it. Extraction-by-pattern is a common mistake; extraction-for-a-reason is the right instinct.

### 3. Synchronous Blocking on I/O Is Not Recoverable By Scaling
**Lesson:** Every user-facing write in Cinemate blocks on a 10-second hate-api call. Adding backend replicas doesn't help — each instance still blocks. The throughput ceiling is set by hate-api (1 worker = 1 inference at a time), not by backend scale. This forced a hard rethink: decouple moderation into async post-publish (pessimistic UX becomes optimistic 202-Accepted), fail-open if moderation is unavailable, and use a worker thread pool for parallelism. The lesson: if a critical service call is on the request's critical path, either the call is not critical or your architecture is wrong.

### 4. Consistency Across Sibling Entities Is Fragile Without Enforcement
**Lesson:** Posts are moderated synchronously at write time; comments bypass moderation and are only cleaned hours later. This inconsistency is by accident (HS-04), not design. When you have two code paths doing "the same thing," they will diverge silently. Better: extract shared logic into a single callable, or enforce at the framework level (e.g., AOP interceptor on content creation).

### 5. Three-Role Auth Systems Become Unmaintainable
**Lesson:** Three separate entity classes (User, Admin, Organization) with role-dispatched login logic, hard-coded per-role endpoints, and a frontend that must "guess" the role to even attempt login. Adding a fourth role requires changes in 5+ places. This is a solved problem: use Spring's `@DiscriminatorColumn` once, use `GrantedAuthority` hierarchy once, and role checks propagate automatically.

### 6. Infrastructure as Code Must Be Exercised Regularly
**Lesson:** The hate-api Dockerfile ignores its own `requirements.txt` (uses a stale layer, never re-run `pip install`). This was invisible in code review — it only surfaced by actually building and running the container. Docker tips: (a) always test `docker compose build` in CI, (b) use BuildKit (forces rebuilds when base image updates), (c) don't rely on layers from previous images.

### 7. Observability Is Not Optional In Distributed Systems
**Lesson:** With four services (backend, watch-party, hate-api, frontend), a user report of "the app is slow" is unanswerable without metrics. Is the backend slow? The hate-api? Network? Just because you can log to stdout doesn't mean you have observability — you need: metrics (latency, throughput, errors), structured logs (queryable by userId, requestId), and tracing (correlating a frontend action to backend calls to microservice calls). This is not a "nice to have" for a polyglot, multi-service system — it's table-stakes for debugging.

### 8. Race Conditions Hide in Read-Modify-Write Patterns
**Lesson:** Vote counts and comment counters in Cinemate were incrementing via read-modify-write (fetch, compute, write) in several places — invisible race conditions under concurrent load. The fix was always the same: use atomic operations (`MongoDB $inc`, `Redis HINCRBY`). The lesson: make "use the database's atomic operators" a team instinct, not an exception.

### 9. Verification Codes Should Be Tokens, Not Enumerable Integers
**Lesson:** 6-digit codes are guessable (900k options, guessed in 2.5 hours at 100 req/sec). Use cryptographic tokens (UUID, SecureRandom), store bcrypt hashes, enforce rate limiting. This pattern applies to anything "secret": API keys, temporary codes, session tokens.

### 10. Test Coverage Requires Intentional Design, Not Just Test Count
**Lesson:** 57 backend test files is a solid starting point, but 7 of them use `@SpringBootTest` + manual mocks (worst of both worlds), and critical paths (security, concurrency, cascade deletion) have zero tests. "We have tests" is not the same as "the critical paths are tested." Recommendation: define "critical" upfront (auth, data integrity, external service failure modes) and ensure 100% coverage on those paths, even if other paths have gaps.

