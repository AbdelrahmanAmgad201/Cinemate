# Cinemate — Open Issues & Lessons Learned

**Status:** Post-implementation audit. Extensive fixes have landed (see `DONE.md`). This document lists only remaining open issues and key architectural lessons.

---

## Open Critical Issues


### HS-01 — Synchronous Moderation Blocks Every Write Operation
**Severity:** Critical | **Effort:** Medium

Posts/forums block on hate-api inference (200–2000ms CPU, 10s timeout). Comments skip write-time moderation entirely. Single uvicorn worker caps platform to ~2 writes/second. Recommended: decouple into async post-publish moderation with `status=PENDING`, fire-and-forget worker thread, update to `PUBLISHED` on safe result. Combine title+content into single HTTP call.



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
