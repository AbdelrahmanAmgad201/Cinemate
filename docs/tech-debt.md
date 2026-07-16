# Known Limitations & Roadmap

Non-blocking, deliberate edges of the current design — not oversights. Each item lists what it
is, why it matters, a suggested fix, and a priority. Nothing here blocks running or extending
the project; resolved items are removed from this list rather than kept as a "done" log (see
git history for that).

---

## Moderation pipeline

The synchronous hate-api was replaced by an async, Kafka-based, optimistic-publish pipeline
with a transactional outbox (see [`moderation.md`](moderation.md)). This is that design's one
remaining open edge.

### MOD-03 — `OutboxRelay` assumes a single backend instance
- **Description:** The relay is an unlocked `@Scheduled` poller. With >1 backend replica, every
  instance publishes every outbox row → duplicate requests.
- **Why it matters:** Idempotency absorbs correctness, but it wastes throughput and muddles
  per-content ordering. Blocks horizontal scaling of the backend.
- **Suggested solution:** A leader lock (e.g. ShedLock) or a `FOR UPDATE SKIP LOCKED`
  claim-before-publish query before scaling out.
- **Priority:** Medium

---

## Architecture


### ARC-08 — No observability infrastructure
- **Description:** No metrics, no distributed tracing, no centralized logging — only
  per-service SLF4J to stdout.
- **Why it matters:** With five services, "the app is slow" is undiagnosable without SSH.
- **Suggested solution:** Add a Prometheus actuator endpoint + JSON logging
  (`logstash-logback-encoder`), and Prometheus + Grafana to `compose.yaml`.
- **Priority:** Medium

