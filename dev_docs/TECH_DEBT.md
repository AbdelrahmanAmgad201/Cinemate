# Cinemate — Technical Debt Backlog

Non-blocking improvements deferred out of the `refactor` branch. Each item lists what
it is, why it matters, a suggested fix, and a priority. Nothing here blocks the merge;
these are the known, deliberate edges of the current design.

> Merge-blocking issues found during the branch audit were fixed directly and are **not**
> listed here.

---

## Moderation pipeline

The synchronous hate-api was replaced by an async, Kafka-based, optimistic-publish
pipeline with a transactional outbox (see [`moderation-architecture.md`](moderation-architecture.md)).
These are that design's own open edges.

### MOD-01 — No reconciliation sweep for content stuck in `PENDING`
- **Description:** The outbox guarantees a moderation request is *published*, but nothing
  re-checks content whose verdict never arrives (a consumer-side failure or bug). Such
  content stays `PENDING` forever — visible, never re-moderated.
- **Why it matters:** It's the missing half of the outbox's durability story; a dropped
  verdict silently leaves potentially-flagged content live.
- **Suggested solution:** A periodic sweep that re-enqueues content in `PENDING` older
  than N minutes. Idempotent — duplicate requests are harmless.
- **Priority:** High

### MOD-02 — Verdict consumer has no retry/DLQ
- **Description:** `ModerationVerdictConsumer` runs with `ack-mode=record` and the default
  error handler: if applying a verdict throws, it retries a few times, then commits the
  offset and moves on — the verdict is lost.
- **Why it matters:** A flagged item can fail to be removed with no trace.
- **Suggested solution:** A `DefaultErrorHandler` with a `DeadLetterPublishingRecoverer`
  (topic `moderation.verdicts.dlq`) or bounded blocking retries.
- **Priority:** High

### MOD-03 — `OutboxRelay` assumes a single backend instance
- **Description:** The relay is an unlocked `@Scheduled` poller. With >1 backend replica,
  every instance publishes every outbox row → duplicate requests.
- **Why it matters:** Idempotency absorbs correctness, but it wastes throughput and
  muddles per-content ordering. Blocks horizontal scaling of the backend.
- **Suggested solution:** A leader lock (e.g. ShedLock) or claim-before-publish before
  scaling out. Already flagged in the `OutboxRelay` javadoc.
- **Priority:** Medium

---

## Architecture

### ARC-02 — Watch-party microservice extraction is premature
- **Description:** The service owns no persistent data (ephemeral Redis), has a hard
  startup dependency, and is called synchronously via `RestTemplate`.
- **Why it matters:** Two services instead of one — more operational complexity and a
  second attack surface — with no realized scaling benefit yet.
- **Suggested solution:** Either fold it back into the monolith as an internal WebSocket
  handler package (keeping Redis pub/sub), or make the calls async via an event queue.
- **Priority:** Low (working as-is; revisit if the topology is simplified)

### ARC-08 — No observability infrastructure
- **Description:** No metrics, no distributed tracing, no centralized logging — only
  per-service SLF4J to stdout.
- **Why it matters:** With five services, "the app is slow" is undiagnosable without SSH.
- **Suggested solution:** Add a Prometheus actuator endpoint + JSON logging
  (`logstash-logback-encoder`), and Prometheus + Grafana to `compose.yaml`.
- **Priority:** Medium

---

## Build, CI, and versions

### TD-BUILD-01 — Heterogeneous Spring Boot / Java versions across modules
- **Description:** backend = Spring Boot 3.5.7 / Java 17; watch-party = Spring Boot 4.0.1
  / Java 17; gateway = Spring Boot 4.1.0 / Java 21. Each module builds independently.
- **Why it matters:** Divergent framework majors and JDKs make shared conventions,
  dependency upgrades, and contributor onboarding harder, and risk subtle behavioural
  drift between services.
- **Suggested solution:** Converge on a single Spring Boot major and JDK (Java 21 +
  Spring Boot 4.x) across all three JVM modules.
- **Priority:** Medium

### TD-BUILD-02 — CI only builds/tests the backend
- **Description:** `.github/workflows/backend-tests.yml` runs only `backend/` tests
  (JDK 17). The gateway and watch-party are neither compiled nor tested in CI, and
  `docker compose build` is never exercised.
- **Why it matters:** A change that breaks the gateway (Java 21) or watch-party, or a
  Dockerfile regression, would pass CI. (Lesson learned in the old hate-api build.)
- **Suggested solution:** Add gateway + watch-party build/test jobs (JDK 21 where
  needed) and a `docker compose build` smoke job.
- **Priority:** Medium

---

## Code quality

### TD-FE-01 — Debug `console.log` left in frontend
- **Description:** ~65 active `console.log` calls (plus ~50 `console.error`) across
  `frontend/src`, many logging response payloads (e.g. `frontend/src/api/*.js`).
- **Why it matters:** Noisy console, leaks response shapes into the browser console, and
  reads as unfinished code in a project that prioritizes quality.
- **Suggested solution:** Strip the data-dumping `console.log`s; keep intentional
  `console.error` in `catch` blocks (or route through a small logger gated on dev mode).
- **Priority:** Low

### TD-LOG-01 — Watch-party `prod` profile logs at DEBUG
- **Description:** `watch-party/src/main/resources/application-prod.properties` sets
  `org.example.watchparty`, `spring.web.socket`, and `spring.messaging` to `DEBUG`.
- **Why it matters:** Verbose, high-volume logs in the production profile — noise and
  overhead, and can leak message contents.
- **Suggested solution:** Lower to `INFO` in the `prod` profile.
- **Priority:** Low

### TD-TEST-01 — Social/vote test suite deleted pending JPA rewrite
- **Description:** The Mongo-era Forum/Post/Comment/Vote/Feed/Moderation service+repo
  tests were removed during the Postgres consolidation and not yet rewritten against JPA.
- **Why it matters:** The social core — the bulk of the domain — currently has thin
  automated coverage.
- **Suggested solution:** Rewrite the service/repository tests against the JPA entities
  using the `AbstractPostgresIntegrationTest` (Testcontainers) base, plus mock-only
  service tests where a container isn't needed.
- **Priority:** High
