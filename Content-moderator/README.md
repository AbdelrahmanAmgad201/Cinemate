# Content Moderator

A Kafka-native toxicity-scoring worker. It consumes moderation requests from a
Kafka topic, scores them in micro-batches with an ONNX model, and produces
verdicts to another topic. It has no HTTP API, no queue of its own, and no
state — Kafka is both the input and the output.

Model: [`minuva/MiniLMv2-toxic-jigsaw-lite`](https://huggingface.co/minuva/MiniLMv2-toxic-jigsaw-lite)
(6-layer MiniLM, ~23M params), served from its pre-quantized ONNX export
([`minuva/MiniLMv2-toxic-jigaw-lite-onnx`](https://huggingface.co/minuva/MiniLMv2-toxic-jigaw-lite-onnx)),
scoring two independent labels: `toxic` and `severe_toxic`. Either label
crossing its threshold sets `flagged: true` — the threshold policy lives here,
not in the caller.

## How it fits into Cinemate

```
backend ──(transactional outbox relay)──▶ moderation.requests ─┐
                                                               │  consumer group
                                                               ▼  "moderation-workers"
                                                    moderation-worker × N
                                                    consume → ONNX score → produce
                                                               │
backend verdict consumer ◀──────────────── moderation.verdicts┘
(idempotent apply: flagged ⇒ remove content)

malformed requests ──▶ moderation.requests.dlq
```

The backend publishes content optimistically (visible immediately) and removes
it retroactively when a verdict comes back flagged. Kafka is the durability
boundary: if the workers are down, requests wait in the topic; if a worker
crashes mid-batch, uncommitted offsets are redelivered.

## Message contracts

JSON values, keyed by `contentId` (per-content ordering through partitioning):

```jsonc
// moderation.requests
{"v": 1, "contentType": "POST", "contentId": "665f...", "version": 3, "text": "..."}

// moderation.verdicts
{"v": 1, "contentType": "POST", "contentId": "665f...", "version": 3,
 "flagged": true, "scores": {"toxic": 0.98, "severe_toxic": 0.02}}
```

`version` is echoed back untouched — the backend uses it to discard verdicts
for text that was edited while the request was in flight.

## Delivery semantics

At-least-once, end to end:

- The producer is idempotent (`enable.idempotence`, implies `acks=all`).
- Offsets are committed **only after** the batch's verdicts are confirmed
  written. Any failure kills the process; the container restarts and the
  consumer group redelivers from the last commit.
- Duplicated verdicts are therefore possible and expected — the backend's
  verdict consumer is idempotent.
- Malformed requests go to the DLQ topic rather than crash-looping a partition.

## Batching

`consume(num_messages=MAX_BATCH, timeout=…)` returns a natural micro-batch,
topped up within `LINGER_MS` — one `inference.score()` call per batch. The
model wrapper (`worker/inference.py`) sorts by text length before padding
(length bucketing) so short comments aren't padded out to a long outlier.

## Configuration (env)

| Var | Default | Effect |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Broker(s) |
| `REQUESTS_TOPIC` / `VERDICTS_TOPIC` / `DLQ_TOPIC` | `moderation.requests` / `.verdicts` / `.requests.dlq` | Topics (created by the backend's KafkaAdmin) |
| `GROUP_ID` | `moderation-workers` | Consumer group |
| `MAX_BATCH` | 32 | Max messages per inference call |
| `LINGER_MS` | 15 | Max wait to top up a partial batch |
| `TOXIC_THRESHOLD` / `SEVERE_TOXIC_THRESHOLD` | 0.5 | Either crossing its threshold ⇒ `flagged: true` |
| `MAX_SEQ_LEN` | 256 | Tokenizer truncation length |
| `ONNX_INTRA_OP_THREADS` / `ONNX_INTER_OP_THREADS` | 1 | Keep low; scale via replicas |

## Running & scaling

The worker runs from the repo-root `compose.yaml`:

```bash
docker compose up -d kafka moderation-worker
docker compose up -d --scale moderation-worker=4
```

Workers are stateless; parallelism is bounded by the partition count of
`moderation.requests` (12) — replicas beyond that sit idle. Consumer-group
**lag** on `moderation.requests` is the one scaling signal that matters:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group moderation-workers
```

## Repo layout

- `worker/inference.py` — ONNX session + tokenizer, batch-native `score(texts)`
- `worker/worker.py` — Kafka consume → score → produce loop
- `model/fetch_model.py` — pinned-revision model fetch, baked into the image at build time
