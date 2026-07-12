# Content Moderation Architecture — Study Guide

> **⚠️ Update (PostgreSQL consolidation):** the pipeline's *shape* is unchanged (optimistic
> publish → transactional outbox → Kafka → worker → verdict consumer), but the outbox is now a
> **PostgreSQL table** (`moderation_outbox`), not a Mongo collection. The content row + outbox row
> commit in **one ordinary Postgres transaction** — so §4.4 below ("two transaction managers"),
> the `MongoTransactionManager`/`mongoTransactionOperations` plumbing, and the **replica-set
> requirement (MOD-07)** are all **gone**. `contentId` is a UUID; the relay reads `findAllByOrderById­Asc`
> over a BIGINT identity PK. The verdict consumer applies status changes via bulk `@Modifying`
> queries (not managed-entity saves). Treat §4.3–4.4 and the replica-set notes as historical; see
> `dev_docs/postgres-consolidation.md`.

A deep walkthrough of how Cinemate moderates user text (posts, comments, forums):
what each piece does, how they fit together, and **why** each design decision was made.
Read it top-to-bottom once; after that the "Component reference" and "Failure modes"
sections are the ones you'll come back to.

---

## 1. The one-paragraph mental model

When a user creates content, we **save it immediately and show it** (`moderationStatus =
PENDING`) — we do *not* wait for a toxicity check. In the *same database transaction* we
also write a little "please moderate this" note into an **outbox** table. A background
**relay** ships those notes to **Kafka**. A fleet of **workers** reads them, runs an ONNX
toxicity model, and writes a **verdict** back to Kafka. The backend reads verdicts and, if
something was flagged, **removes it retroactively**. Nothing a user does ever blocks on the
model, and no note is ever lost even if the app crashes at the worst possible moment.

That's the whole thing. The rest of this document explains each italicized word.

---

## 2. Why it's built this way (the three big decisions)

Before the mechanics, understand the three choices that shape everything else.

### 2.1 Optimistic publish (not "hold until approved")
Content is **visible the instant it's saved**; a flagged verdict removes it a second or two
later. The alternative — hold content as `PENDING` and only show it once approved — makes
*every* post feel laggy and makes a moderation backlog a product outage.

- **Trade-off we accepted:** toxic content is briefly visible (seconds).
- **Why it's fine here:** this is how Reddit/YouTube-style platforms work; the removal is
  fast and automatic, and the nightly-cleanup instinct already lived in the old code.

### 2.2 Asynchronous, off the write path
The old system called a hate-speech HTTP API **synchronously** inside `addPost()` — every
write blocked 200–2000ms (10s timeout) on one single-threaded model. Adding backend
replicas didn't help: the model was the ceiling. Moving moderation onto a queue means the
write path is bounded by the database, and the model fleet scales independently.

### 2.3 Transactional outbox for crash-safety
The scary bug in any "save to DB, then send to a queue" design is the **dual-write
problem** (§5). We solve it by writing the content and the "moderate me" request into the
**same Mongo transaction**, then relaying to Kafka separately. Either both the content and
its moderation request exist, or neither does — never one without the other.

---

## 3. The end-to-end flow

```
                          ┌─────────────────────── BACKEND (Spring Boot) ───────────────────────┐
                          │                                                                      │
  user ── POST /forum ───▶│  ForumService.createForum()                                          │
                          │     │                                                                │
                          │     │  ┌──────── ONE Mongo transaction ────────┐                     │
                          │     └─▶│  save Forum (status=PENDING, ver=1)    │                     │
                          │        │  save ModerationOutboxEntry            │                     │
                          │        └────────────────────────────────────────┘                    │
                          │                        │ (content is now visible; 200 OK returned)    │
                          │                        ▼                                              │
                          │     OutboxRelay  @Scheduled every 1s                                  │
                          │        reads outbox rows oldest-first                                 │
                          │        publishes → deletes only after broker ack                      │
                          └───────────────────────────┬──────────────────────────────────────────┘
                                                      │ key = contentId
                                                      ▼
                                    ┌───────────────────────────────────┐
                                    │  Kafka topic: moderation.requests │  (12 partitions)
                                    └──────────────────┬────────────────┘
                                                       │ consumer group "moderation-workers"
                                                       ▼
                          ┌──────────────── moderation-worker × N (Python) ────────────────┐
                          │  consume up to 32 msgs  →  ONNX score(batch)  →  produce verdict │
                          │  commit offset only AFTER verdicts are written                  │
                          │  malformed message → moderation.requests.dlq                    │
                          └──────────────────────────┬─────────────────────────────────────┘
                                                     │ key = contentId
                                                     ▼
                                    ┌───────────────────────────────────┐
                                    │  Kafka topic: moderation.verdicts │  (12 partitions)
                                    └──────────────────┬────────────────┘
                                                       │ consumer group "backend-moderation"
                                                       ▼
                          ┌──────────────── BACKEND: ModerationVerdictConsumer ────────────┐
                          │  flagged?  →  version matches?  →  soft-delete + cascade        │
                          │  clean?    →  mark APPROVED (version-guarded, idempotent)       │
                          └────────────────────────────────────────────────────────────────┘
```

**Two independent async hops:** backend → `moderation.requests` → worker, and worker →
`moderation.verdicts` → backend. The backend is both a **producer** (of requests, via the
relay) and a **consumer** (of verdicts). The worker is the mirror image.

---

## 4. Component reference (the "how")

Every path below is relative to the repo root.

### 4.1 Domain model — the moderation state on content
Files: [`moderation/Moderatable.java`](../backend/src/main/java/org/example/backend/moderation/Moderatable.java),
[`ModerationStatus.java`](../backend/src/main/java/org/example/backend/moderation/ModerationStatus.java),
[`ContentType.java`](../backend/src/main/java/org/example/backend/moderation/ContentType.java)

Every moderatable document (`Post`, `Comment`, `Forum`) implements `Moderatable` and gains
two fields:

| Field | Purpose |
|---|---|
| `moderationStatus` | `PENDING` → `APPROVED` / `REMOVED`. Content is visible in **every** state; `REMOVED` also soft-deletes it. Legacy docs with `null` are treated as `APPROVED`. |
| `moderationVersion` | Starts at 1, **incremented on every edit**. Lets a late verdict for old text be discarded (§6). |

`Moderatable` exists so the verdict consumer can apply a verdict to any content type
generically (`getModerationVersion()`, `setModerationStatus()`, `getIsDeleted()`) without a
`switch` on concrete types for the field access.

### 4.2 The outbox table
Files: [`ModerationOutboxEntry.java`](../backend/src/main/java/org/example/backend/moderation/ModerationOutboxEntry.java),
[`ModerationOutboxRepository.java`](../backend/src/main/java/org/example/backend/moderation/ModerationOutboxRepository.java)

A row in the `moderation_outbox` collection is a durable "publish this to Kafka later"
instruction:

```
{ _id, contentType, contentId, contentVersion, text (snapshot), createdAt }
```

- `_id` is a Mongo `ObjectId`, which is **time-ordered**, so reading `findAllByOrderByIdAsc`
  replays entries in the order they were created (important for per-content edit order).
- `text` is a **snapshot** taken at write time. The worker never reads our database — it
  moderates exactly the text that was written, which matters when content is edited (§6).

### 4.3 Enqueue — writing the outbox row
File: [`ModerationOutboxService.java`](../backend/src/main/java/org/example/backend/moderation/ModerationOutboxService.java)

`enqueue(contentType, contentId, version, text)` just saves one outbox entry. The
**contract** is that it must be called **inside the same Mongo transaction as the content
save** — that's what makes the outbox atomic. The caller owns the transaction.

### 4.4 Two transaction managers — the subtle bit
File: [`config/TransactionConfig.java`](../backend/src/main/java/org/example/backend/config/TransactionConfig.java)

Cinemate has two datastores: **MySQL** (JPA — users, movies) and **MongoDB** (posts,
comments, forums, and now the outbox). The moderation outbox needs a **Mongo**
transaction. Adding a `MongoTransactionManager` naively would break every existing
unqualified `@Transactional` in the codebase, because Spring Boot auto-configures a JPA
transaction manager *only when no other `TransactionManager` bean exists*. So we define
both explicitly:

- `JpaTransactionManager` is `@Primary` → every existing `@Transactional` keeps using MySQL,
  exactly as before.
- `MongoTransactionManager` is a second, non-primary bean.
- `mongoTransactionOperations` is a `TransactionTemplate` around the Mongo manager. Services
  call `mongoTransactionOperations.execute(...)` to run the content-save + outbox-write
  atomically.

> **Why injection is unambiguous:** Boot's own auto `TransactionTemplate` is
> `@ConditionalOnMissingBean(TransactionOperations.class)`. Because we declare
> `mongoTransactionOperations` (a `TransactionOperations`), Boot backs off and ours is the
> *only* bean of that type — so constructor injection resolves cleanly by type.

> **Why Mongo transactions need a replica set:** MongoDB multi-document transactions are only
> available on a replica set (or sharded cluster), never on a standalone `mongod`. That's why
> `compose.yaml` runs Mongo with `--replSet rs0` and initiates it in the healthcheck. Point
> the backend at a standalone Mongo and **every content write fails**. (See MOD-07 in
> `FINAL_AUDIT.md`.)

### 4.5 The services — optimistic publish
Files: [`post/PostService.java`](../backend/src/main/java/org/example/backend/post/PostService.java),
[`comment/CommentService.java`](../backend/src/main/java/org/example/backend/comment/CommentService.java),
[`forum/ForumService.java`](../backend/src/main/java/org/example/backend/forum/ForumService.java)

Each create/update follows the same shape (using `createForum` as the example):

```java
Forum forum = Forum.builder()...build();          // moderationStatus defaults to PENDING, version 1
Forum saved = mongoTransactionOperations.execute(status -> {
    Forum s = forumRepository.save(forum);         // content — visible immediately
    moderationOutboxService.enqueue(               // outbox — same transaction
        ContentType.FORUM, s.getId().toHexString(),
        s.getModerationVersion(),
        moderationText(s.getName(), s.getDescription()));
    return s;
});
```

Key points:
- **No synchronous moderation, no `HateSpeechException`.** The old blocking call is gone.
- **Title + content (or name + description) are moderated as one snapshot** joined by `\n` —
  one model pass covers both fields, and there's no ambiguity about which field a verdict
  refers to.
- **Edits** (`updatePost`, `updateForum`) bump `moderationVersion`, reset status to
  `PENDING`, and enqueue the new version — the edited text gets re-moderated.
- **`systemDeletePost` / `systemDeleteForum` / `systemDeleteComment`** are removal methods
  *without* an ownership check — they exist for the verdict consumer to remove flagged
  content (a user isn't "deleting" it; the system is).

### 4.6 The publisher: OutboxRelay
File: [`moderation/OutboxRelay.java`](../backend/src/main/java/org/example/backend/moderation/OutboxRelay.java)

This is the bridge from the durable outbox to Kafka. It runs on a timer:

```java
@Scheduled(fixedDelayString = "${moderation.outbox.relay-delay-ms:1000}")
public void relay() {
    List<ModerationOutboxEntry> batch = outboxRepository.findAllByOrderByIdAsc(PageRequest.of(0, batchSize));
    for (entry : batch) {
        String payload = objectMapper.writeValueAsString(ModerationRequestMessage.from(entry));
        kafkaTemplate.send(requestsTopic, entry.getContentId(), payload).get(timeout);  // BLOCK until ack
        outboxRepository.deleteById(entry.getId());                                      // delete AFTER ack
    }
}
```

Why each detail matters:

- **Poll oldest-first (`findAllByOrderByIdAsc`)** — preserves creation order, which combined
  with keying by `contentId` keeps a content item's edits in order.
- **Key the Kafka message by `contentId`** — Kafka guarantees ordering *within a partition*,
  and all messages with the same key go to the same partition. So two edits of the same post
  are delivered to the worker in order.
- **Blocking send + `.get()`** — we wait for the broker to acknowledge before deleting the
  outbox row. This is **delete-after-ack**: if the process dies after `send` but before
  `delete`, the row survives and is re-published next tick. That's what makes delivery
  **at-least-once** (§5).
- **Stop at the first failed send** (`return`) — if Kafka is down, we stop and retry the
  whole batch next tick, preserving order rather than skipping ahead. Content creation is
  unaffected; requests just pile up in the outbox until the broker returns.
- **Unserializable rows are dropped**, not retried forever — they can never succeed.

> **Known limitation (MOD-03):** the relay is an unlocked `@Scheduled` poller, so it assumes a
> **single backend instance**. With multiple replicas, each would publish every row (duplicate
> requests). Idempotency (§5) keeps that *correct*, but it's wasteful — a leader lock
> (ShedLock) is needed before scaling the backend horizontally.

### 4.7 The wire contracts
Files: [`ModerationRequestMessage.java`](../backend/src/main/java/org/example/backend/moderation/ModerationRequestMessage.java),
[`ModerationVerdictMessage.java`](../backend/src/main/java/org/example/backend/moderation/ModerationVerdictMessage.java)

JSON, keyed by `contentId`. Request (backend → worker):

```json
{ "v": 1, "contentType": "POST", "contentId": "665f…", "version": 3, "text": "…" }
```

Verdict (worker → backend):

```json
{ "v": 1, "contentType": "POST", "contentId": "665f…", "version": 3,
  "flagged": true, "scores": { "toxic": 0.98, "severe_toxic": 0.02 } }
```

- `version` is **echoed untouched** by the worker — the backend uses it to detect stale
  verdicts (§6).
- The verdict record uses **boxed types** (`Boolean`, `Long`) and `@JsonIgnoreProperties`, so a
  missing field deserializes to `null` and is rejected explicitly rather than silently
  defaulting (e.g. a missing `flagged` must not read as `false`).

### 4.8 Topic configuration
File: [`moderation/ModerationKafkaConfig.java`](../backend/src/main/java/org/example/backend/moderation/ModerationKafkaConfig.java)

Declares three topics as `NewTopic` beans; Spring's `KafkaAdmin` creates them at backend
startup (broker auto-create is **off** so partition counts stay deliberate):

| Topic | Partitions | Role |
|---|---|---|
| `moderation.requests` | 12 | work queue backend → workers |
| `moderation.verdicts` | 12 | results workers → backend |
| `moderation.requests.dlq` | 1 | poison messages (malformed requests) |

**Partition count = maximum worker parallelism.** Consumers in a group can't exceed the
partition count, so 12 leaves room to scale workers without repartitioning. `replicas=1`
matches the single-broker dev setup; production would use RF=3 with `min.insync.replicas=2`.

### 4.9 The worker (publisher of verdicts, consumer of requests)
Files: [`Content-moderator/worker/worker.py`](../Content-moderator/worker/worker.py),
[`inference.py`](../Content-moderator/worker/inference.py)

This is a standalone Python service (not Spring). The main loop:

```python
while True:
    batch = consumer.consume(num_messages=MAX_BATCH, timeout=1s)   # natural micro-batch
    if len(batch) < MAX_BATCH:
        batch += consumer.consume(MAX_BATCH - len(batch), LINGER_MS/1000)  # top up briefly
    flush(consumer, producer, batch)
```

`flush()`:
1. Parse each message. **Malformed → produce to the DLQ** (don't crash the partition).
2. `inference.score([...texts])` — **one ONNX call for the whole batch**.
3. For each result: `flagged = toxic >= TOXIC_THRESHOLD or severe_toxic >= SEVERE_TOXIC_THRESHOLD`.
4. Produce each verdict, keyed by the same `contentId`.
5. `producer.flush()`; if any delivery failed, **raise** (die without committing → redelivery).
6. Only if all verdicts are durable: `consumer.commit()`.

Why these choices:
- **`enable.auto.commit=false` + commit-after-produce** — the offset only advances once
  verdicts are safely in Kafka. Crash mid-batch → the whole batch is redelivered
  (at-least-once).
- **Idempotent producer (`enable.idempotence=True`, implies `acks=all`)** — no silent loss or
  in-session duplication of a verdict.
- **Micro-batching** — `consume()` returns many messages at once; batching amortizes model
  overhead. `inference.py` sorts the batch by text length before padding ("length bucketing")
  so a short comment isn't padded out to a long outlier's length.
- **The threshold logic lives here**, driven by env vars — the backend just consumes `flagged`.

`inference.py` loads the ONNX session + tokenizer **once per process** and exposes exactly
one function, `score(texts) -> [{"toxic":…, "severe_toxic":…}]`. The model
(`minuva/MiniLMv2-toxic-jigsaw-lite`) is baked into the image at build time (see the worker
Dockerfile), so there's no network fetch at runtime.

### 4.10 The verdict consumer
File: [`moderation/ModerationVerdictConsumer.java`](../backend/src/main/java/org/example/backend/moderation/ModerationVerdictConsumer.java)

```java
@KafkaListener(topics = "${moderation.topics.verdicts}")
public void onVerdict(String payload) { … }
```

It parses/validates the verdict, resolves the `ContentType`, and dispatches:

- **Clean verdict** → one atomic, **version-guarded** update:
  ```
  updateFirst(where _id == id AND moderationVersion == version AND status == PENDING,
              set status = APPROVED)
  ```
  If the content was edited since (version moved on), or already resolved, this matches
  nothing — a **no-op**. No read, no delete.
- **Flagged verdict** → `findById`; then:
  - content missing or already `isDeleted` → **no-op** (idempotent: a redelivered verdict does
    nothing).
  - `moderationVersion != verdict.version` → **stale, ignore** (the text was edited; the newer
    request's verdict is authoritative).
  - otherwise → set `status = REMOVED`, then call the type's `systemDelete…` to soft-delete +
    cascade.

**`ack-mode=record`** (in `application.properties`) means the Kafka offset is committed
*after* `onVerdict` returns — so a crash mid-apply redelivers the verdict rather than losing
it. This is why every branch above must be **idempotent**.

### 4.11 Configuration reference
File: [`application.properties`](../backend/src/main/resources/application.properties)

```properties
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.producer.acks=all                       # relay: no acked message lost
spring.kafka.producer.properties.enable.idempotence=true
spring.kafka.consumer.group-id=backend-moderation
spring.kafka.consumer.auto-offset-reset=earliest     # process backlog on fresh group
spring.kafka.consumer.enable-auto-commit=false
spring.kafka.listener.ack-mode=record                # commit after each verdict handled

moderation.topics.requests=moderation.requests
moderation.topics.verdicts=moderation.verdicts
moderation.topics.dlq=moderation.requests.dlq
moderation.topics.partitions=12
moderation.outbox.batch-size=100
moderation.outbox.relay-delay-ms=1000
```

Worker env (defaults in `compose.yaml`): `KAFKA_BOOTSTRAP_SERVERS`, `MAX_BATCH=32`,
`LINGER_MS=15`, `TOXIC_THRESHOLD=0.5`, `SEVERE_TOXIC_THRESHOLD=0.5`.

---

## 5. Delivery semantics & the transactional outbox (the core "why")

This section is the conceptual heart — the part worth truly understanding.

### 5.1 The dual-write problem
Naively, moderation would be: `save content to Mongo; then send request to Kafka`. But
these are two separate systems with no shared transaction. If the process crashes *between*
them, you get a post that exists but was never moderated — **silent, permanent**. Retrying
the Kafka send first and saving after just inverts the failure (a moderation request for a
post that doesn't exist).

There is no way to make "write to DB" and "write to Kafka" atomic directly. This is a
fundamental distributed-systems problem, not a Cinemate quirk.

### 5.2 How the outbox solves it
Split the operation into two steps, each individually safe:

1. **Content + outbox row** committed in **one Mongo transaction.** Atomic by construction —
   both or neither. No Kafka involved yet.
2. **A separate relay** reads the outbox and publishes to Kafka, deleting each row **only
   after** the broker acknowledges.

Now trace the crash points:
- Crash during step 1 → transaction rolls back → no content, no outbox row. Clean.
- Crash after step 1, before relay → content exists, outbox row exists → relay publishes it
  next tick. Nothing lost.
- Crash after `send` but before `deleteById` → row survives → relay re-publishes → a
  **duplicate** request. Harmless (§5.3).

The database is the single source of truth; Kafka is fed *from* it, never written beside it.

### 5.3 At-least-once + idempotency (not exactly-once)
Every hop here is **at-least-once**: the relay can double-publish (crash before delete), and
the worker can double-produce (crash before commit). We deliberately do **not** chase
Kafka's exactly-once semantics (EOS) — it's heavy machinery. Instead we make **duplicates
harmless** by making every effect **idempotent**:

- Applying "flagged → remove" twice = the second one sees `isDeleted` and no-ops.
- Applying "clean → APPROVED" twice = the version-guarded update matches nothing the second
  time.
- Keying everything by `contentId` keeps a content item's messages ordered and colocated.

> **The lesson:** knowing *when you don't need* exactly-once is more valuable than being able
> to configure it. Idempotent operations + at-least-once delivery is simpler and just as
> correct here.

### 5.4 Where durability is strong vs. soft
- **Strong (ingress):** once a content write commits, its moderation request *will* reach
  Kafka (outbox + delete-after-ack). Broker downtime only delays it.
- **Soft (egress):** if a *verdict* is lost — e.g. the verdict consumer throws past its retry
  budget (MOD-02), or a bug — the content stays `PENDING` forever with nothing re-checking
  it. The missing safety net is a **reconciliation sweep** (MOD-01) that re-enqueues
  long-`PENDING` content. This is the top follow-up item.

---

## 6. Ordering & stale verdicts (the version guard)

Content can be **edited while its moderation request is in flight**. Example:

1. User posts "helo wrld" → version 1 → request v1 sent.
2. User edits to "hello world" → version 2, status back to `PENDING` → request v2 sent.
3. Verdict for **v1** arrives (maybe it flagged a typo-obscured slur) → but the current text
   is v2.

Applying the v1 verdict would moderate text that no longer exists. The guard: the verdict
carries the `version` it was computed for; the consumer only acts if
`content.moderationVersion == verdict.version`. The v1 verdict is discarded as **stale**; the
v2 verdict is authoritative.

Keying Kafka messages by `contentId` means v1 and v2 requests (and their verdicts) stay in
one partition, processed in order — but the version guard is the real correctness
mechanism; ordering is just a helpful assist.

---

## 7. Failure modes — what happens when X breaks

| Failure | Behavior | Data safe? |
|---|---|---|
| Kafka broker down | Relay can't publish → outbox rows accumulate → retried each tick. Content creation unaffected (it never touches Kafka). | ✅ requests wait durably |
| Backend crashes after content save, before relay | Outbox row survives the restart → published next tick. | ✅ |
| Backend crashes after Kafka `send`, before outbox delete | Row survives → re-published → duplicate request → idempotent verdict. | ✅ |
| Worker crashes mid-batch (before commit) | Offset not committed → batch redelivered → possibly duplicate verdicts → idempotent apply. | ✅ |
| Worker gets a malformed request | Routed to `moderation.requests.dlq`; worker keeps running. | ✅ (isolated) |
| Verdict consumer throws (e.g. Mongo blip) | Default error handler retries, then **commits and moves on** → verdict lost, flagged item not removed. **(MOD-02 — open)** | ⚠️ egress soft spot |
| Verdict simply never arrives (bug/loss) | Content stuck `PENDING` forever, visible, never re-checked. **(MOD-01 — no reconciliation sweep yet)** | ⚠️ |
| Mongo is standalone (not a replica set) | Every content write fails (outbox transaction unsupported). **(MOD-07)** | ❌ writes rejected loudly |
| Two backend replicas | Both relays publish every row → duplicate requests → idempotent but wasteful. **(MOD-03)** | ✅ correct, wasteful |

---

## 8. Dependencies

**Backend (Java):**
- `spring-kafka` — `KafkaTemplate` (relay producer), `@KafkaListener` (verdict consumer),
  `KafkaAdmin` (topic creation).
- `spring-boot-starter-data-mongodb` — documents, repositories, `MongoTransactionManager`.
- Jackson (via Boot) — request/verdict JSON.

**Worker (Python):** [`Content-moderator/worker/requirements.txt`](../Content-moderator/worker/requirements.txt)
- `confluent-kafka` — consumer/producer.
- `onnxruntime` + `tokenizers` + `numpy` — model inference.

**Infrastructure** ([`compose.yaml`](../compose.yaml)):
- **Kafka** `apache/kafka:3.9.1` in **KRaft** mode (no ZooKeeper) — single-node, internal-only.
- **MongoDB** as a single-node **replica set** (`--replSet rs0`) — required for the outbox
  transaction; the healthcheck initiates the set on first boot.
- **moderation-worker** — built from `Content-moderator/`, the model baked in at build time.

The backend deliberately does **not** `depends_on: kafka` — optimistic publish means the app
must boot and serve writes even when the moderation pipeline is down (the outbox just fills
up and drains later).

---

## 9. Glossary

- **Transactional outbox** — pattern where a message-to-be-sent is written in the same DB
  transaction as the state change, then relayed to the broker separately. Solves the
  dual-write problem.
- **Dual-write problem** — the impossibility of atomically writing to two systems (DB + queue)
  without a shared transaction.
- **At-least-once** — every message is delivered one or more times (never zero); duplicates
  are possible.
- **Idempotent** — applying the same operation twice has the same effect as applying it once.
- **Consumer group** — a set of consumers sharing a subscription; Kafka splits the topic's
  partitions among them, so each message is processed by exactly one member.
- **Offset** — a consumer's bookmark in a partition. Committing an offset says "I've durably
  processed up to here."
- **Partition** — an ordered, independently-consumable shard of a topic. Ordering is
  guaranteed only *within* a partition; the message key decides the partition.
- **KRaft** — Kafka's built-in consensus (Raft) that replaces ZooKeeper for metadata.
- **Micro-batching** — accumulating several messages and processing them in one shot
  (here, one ONNX call per batch) to amortize per-item overhead.
- **Length bucketing** — sorting a batch by text length before padding so short inputs aren't
  padded to a long outlier's length, cutting wasted compute.

---

## 10. Self-check questions

If you can answer these, you understand the system:

1. Why can't we just `save(); kafkaTemplate.send();` and skip the outbox entirely?
2. The relay crashes right after Kafka acknowledges a send but before it deletes the outbox
   row. What happens on restart, and why is it safe?
3. A user edits a post twice in quick succession. Three verdicts arrive out of order. Which
   one wins, and what mechanism enforces that?
4. Why does the worker commit its Kafka offset *after* producing verdicts rather than before?
5. Why is a MongoDB replica set a hard requirement now, when it wasn't before?
6. Where is the durability guarantee strong, and where is it soft? What single component would
   close the soft gap?
7. Why did we choose at-least-once + idempotency over Kafka's exactly-once semantics?
8. What breaks if you run two backend replicas today, and why is it a performance problem
   rather than a correctness one?

---

## 11. Related documents
- [`FINAL_AUDIT.md`](FINAL_AUDIT.md) — remaining work items (MOD-01…08) and lessons learned.
- [`Content-moderator/README.md`](../Content-moderator/README.md) — the worker service in isolation.
- [`docs/environment.md`](../docs/environment.md) — env vars and deployment.
