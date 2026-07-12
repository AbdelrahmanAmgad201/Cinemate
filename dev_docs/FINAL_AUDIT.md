# Cinemate — Open Issues & Lessons Learned

**Status:** Post-implementation audit. Extensive fixes have landed (see `DONE.md`). This document lists only remaining open issues and key architectural lessons.

---

## Open High-Priority Issues

### ARC-01 — Dual-Database Split (MySQL + MongoDB) With No Transaction Boundary
**Severity:** High | **Effort:** High | **✅ RESOLVED** (chose fix option (a))

MySQL holds relational data (User, Movie, Organization, WatchParty), MongoDB holds social content (Post, Comment, Vote, Forum). WatchParty lives in both with separate write paths and no compensation on failure. If `callMicroserviceInitialize()` throws after MySQL commit, the party exists in MySQL but not in Redis.

**Fix options:** (a) consolidate to single database; (b) add compensating cleanup (rollback MySQL on microservice failure); (c) introduce saga/outbox pattern for cross-store operations.

**✅ Resolved:** the persistence layer was consolidated to a single PostgreSQL database
(`dev_docs/postgres-consolidation.md`). MySQL + MongoDB are gone; there is no cross-store boundary,
and the moderation outbox now commits atomically with its content in one ordinary transaction. The
WatchParty ↔ Redis dual-write remains (that's the watch-party microservice, ARC-02 — out of scope
for the DB consolidation), but the MySQL/MongoDB half of the problem is eliminated.

### ARC-02 / ARCH-NEW-02 — Watch-Party Microservice Extraction Is Premature
**Severity:** Medium | **Effort:** Medium

Service owns no persistent data (ephemeral Redis), has hard startup dependency (`depends_on: condition: service_healthy`), and is called **synchronously** via `RestTemplate`. Contains ~130 lines of HTTP boilerplate and no independent scaling benefit realized yet. Also lacks STOMP-level auth (REL-08) and has near-zero test coverage (TEST-04).

**Recommendation:** Fold back into monolith as internal `WatchPartyWebSocketHandler` package (keeps Redis pub/sub), or make calls async via event queue.

### ARC-08 — No Observability Infrastructure
**Severity:** Medium | **Effort:** Medium

No metrics, no distributed tracing, no centralized logging. Only per-service SLF4J to stdout. When the system degrades, there is no way to diagnose without SSH.

**Fix:** Add `spring-boot-starter-actuator` + Prometheus endpoint, add `logstash-logback-encoder` for JSON logging, add Prometheus + Grafana to compose.yaml.

---

## Content Moderation (Kafka Pipeline) — Remaining Work

The synchronous hate-api was replaced by an async, Kafka-based, optimistic-publish
pipeline with a transactional outbox (see the `moderation` package + `Content-moderator`
worker). This resolved HS-01 (synchronous blocking), HS-06 / CQ-11 (the nightly
O(N) scheduler — deleted), and HS-03/HS-07 (title+content combined into one request).
The following are the new feature's own open edges.

### MOD-01 — No reconciliation sweep for content stuck in `PENDING`
**Severity:** High | **Effort:** Low

The outbox guarantees a request is *published*, but nothing re-checks content whose
**verdict never arrives** (lost in a consumer-side failure — see MOD-02 — or a bug).
Such content stays `PENDING` forever: visible, never re-moderated. The old nightly
scheduler was a crude safety net; it's gone with nothing replacing it. **Fix:** a
periodic sweep that re-enqueues content in `PENDING` older than N minutes (idempotent —
duplicate requests are harmless). This is the missing half of the outbox's durability story.

### MOD-02 — Verdict consumer has no retry/DLQ; a throwing handler drops verdicts
**Severity:** High | **Effort:** Low-Medium

`ModerationVerdictConsumer` runs with `ack-mode=record` and Spring's default error
handler: if applying a verdict throws (e.g. Mongo unavailable mid-`systemDelete`), it
retries a few times, then **commits the offset and moves on** — the verdict is lost, and
a flagged item is never removed. **Fix:** a `DefaultErrorHandler` with a
`DeadLetterPublishingRecoverer` (a `moderation.verdicts.dlq`) or bounded blocking retries.

### MOD-03 — `OutboxRelay` assumes a single backend instance
**Severity:** Medium | **Effort:** Low

The relay is an unlocked `@Scheduled` poller. With >1 backend replica, every instance
publishes every outbox row → duplicate requests (idempotency absorbs correctness, but
it wastes throughput and muddles per-content ordering). **Fix:** a leader lock (ShedLock)
or a claim-before-publish before scaling the backend horizontally. (Already flagged in
the `OutboxRelay` javadoc.)

### MOD-04 — Head-of-line blocking in the relay
**Severity:** Low | **Effort:** Low

The relay stops at the first failed publish to preserve order, so one persistently
failing entry (e.g. a payload the broker rejects) blocks all newer entries. Only JSON
*serialization* failures are dropped today. **Fix:** a per-entry attempt counter → move
to a poison table after K tries.

### MOD-05 — No pipeline observability
**Severity:** Medium | **Effort:** Medium

Dropping the moderator's Prometheus/Grafana left the pipeline blind: no consumer-group
**lag**, verdict latency, or DLQ-size signal. If workers fall behind, `PENDING` content
just lingers with no alert. Minimal: scrape Kafka consumer-group lag + DLQ record count.
Subsumes the moderation slice of ARC-08.

### MOD-06 — Frontend gives no moderation feedback
**Severity:** Low | **Effort:** Low-Medium

The create flow doesn't surface newly created content, and moderation-removed content
vanishes with no explanation (see the forum-creation UX report). **Fix:** expose
`moderationStatus` in the view DTOs → "pending review" / "removed by moderation" states.

### MOD-07 — Operational: Mongo now *requires* a replica set
**Severity:** Medium (deployment) | **Effort:** n/a

The outbox commits content + outbox entry in one Mongo transaction, which only works on
a replica set. `compose.yaml` runs single-node `rs0` and initiates it via healthcheck,
but any environment pointing the backend at a standalone `mongod` will fail **every**
content write. Document this as a hard deployment prerequisite.

### MOD-08 — Notes (working as designed, but load-bearing)
- **Duplicate verdicts on worker restart** are expected (offset committed after produce;
  idempotent producer only dedups within a session). Safe *only* because verdict
  application is idempotent (version guard + already-removed no-op) — keep it that way.
- **First-boot replay:** the verdict consumer uses `auto-offset-reset=earliest`, so a
  fresh group reprocesses the topic's full retention on first start (idempotent, but
  could re-remove content an admin manually restored). Consider `latest` once established.

---

## Architectural Lessons Learned

### 1. Dual-Database Splits Without Transaction Boundaries Cause Silent Data Loss
**Lesson:** The MySQL + MongoDB split in Cinemate (relational data in MySQL, social content in MongoDB, WatchParty spanning both) has no cross-store atomicity. If any microservice HTTP call fails after a local commit, the data is orphaned. This is not a "nice to have" in distributed systems — it's a correctness requirement that must be decided upfront. Choose: (a) consolidate to one database, (b) saga/outbox pattern with compensating transactions, or (c) accept the data-loss risk.

### 2. Premature Microservice Extraction Increases Complexity Without Scaling
**Lesson:** The watch-party service was extracted to "enable scaling," but it owns no persistent data, has a hard startup dependency, and is called synchronously. The result: more operational complexity (two services instead of one), a second attack surface (WebSocket auth, Redis keys, inter-service communication), and zero realized scaling benefit. The decision to extract should have been deferred until one of these was true: (a) the service had its own data store, (b) it was genuinely called asynchronously, or (c) there was a concrete scaling bottleneck that required it. Extraction-by-pattern is a common mistake; extraction-for-a-reason is the right instinct.

### 3. Synchronous Blocking on I/O Is Not Recoverable By Scaling
**Lesson:** Every user-facing write in Cinemate blocked on a 10-second hate-api call. Adding backend replicas didn't help — each instance still blocked. The throughput ceiling was set by hate-api (1 worker = 1 inference at a time), not by backend scale. This forced a hard rethink: decouple moderation into async post-publish (pessimistic UX becomes optimistic publish), and use a batch-native worker fleet for parallelism. The lesson: if a critical service call is on the request's critical path, either the call is not critical or your architecture is wrong.

**✅ Resolved:** moderation is now off the write path entirely — content publishes optimistically (`PENDING`, visible immediately) and a Kafka worker fleet moderates asynchronously, removing flagged content retroactively. Writes are bounded by Mongo, not by inference.

### 4. Consistency Across Sibling Entities Is Fragile Without Enforcement
**Lesson:** Posts were moderated synchronously at write time; comments bypassed moderation and were only cleaned hours later. This inconsistency was by accident (HS-04), not design. When you have two code paths doing "the same thing," they will diverge silently. Better: extract shared logic into a single callable, or enforce at the framework level (e.g., AOP interceptor on content creation).

**✅ Resolved:** posts, comments, and forums now all flow through one path — `ModerationOutboxService.enqueue()` at write time and `ModerationVerdictConsumer` for removal. No content type can silently skip moderation. (A `Moderatable` interface + shared enqueue is the "single callable"; an AOP interceptor remains a possible future tightening.)

### 5. Three-Role Auth Systems Become Unmaintainable
**Lesson:** Three separate entity classes (User, Admin, Organization) with role-dispatched login logic, hard-coded per-role endpoints, and a frontend that must "guess" the role to even attempt login. Adding a fourth role requires changes in 5+ places. This is a solved problem: use Spring's `@DiscriminatorColumn` once, use `GrantedAuthority` hierarchy once, and role checks propagate automatically.

### 6. Infrastructure as Code Must Be Exercised Regularly
**Lesson:** The hate-api Dockerfile ignores its own `requirements.txt` (uses a stale layer, never re-run `pip install`). This was invisible in code review — it only surfaced by actually building and running the container. Docker tips: (a) always test `docker compose build` in CI, (b) use BuildKit (forces rebuilds when base image updates), (c) don't rely on layers from previous images.

### 7. Observability Is Not Optional In Distributed Systems
**Lesson:** With four services (backend, watch-party, hate-api, frontend), a user report of "the app is slow" is unanswerable without metrics. Is the backend slow? The hate-api? Network? Just because you can log to stdout doesn't mean you have observability — you need: metrics (latency, throughput, errors), structured logs (queryable by userId, requestId), and tracing (correlating a frontend action to backend calls to microservice calls). This is not a "nice to have" for a polyglot, multi-service system — it's table-stakes for debugging.
