# Cinemate — Measured Performance (Latency & Throughput)

**Status:** ✅ Measured, reproducible — this document reports numbers **actually observed**
on the running stack (as opposed to target/aspirational SLOs).

**Date measured:** 2026-07-15
**Harness:** `bench_inference.py` (ONNX path) + `bench_api.py` (post API + end-to-end verdict).

## Test environment (report this alongside every number)

Single developer machine — Docker Desktop on Windows 11, all services in one Compose
network. Deliberately modest, per-service resource caps (from `compose.yaml`), so these
are **Tier-1 / single-node** numbers, not a distributed cloud deployment.

| Component | Constraint |
|---|---|
| Backend (Spring Boot) | 2 vCPU, JVM `-Xmx512m` |
| Moderation worker | 1 replica, ONNX `intra_op=1 / inter_op=1` thread, 2 vCPU cap |
| Model | `minuva/MiniLMv2-toxic-jigsaw-lite`, **INT8-quantized ONNX** (6-layer MiniLM, ~23M params), `model_optimized_quantized.onnx`, `MAX_SEQ_LEN=256` |
| Postgres / Kafka / Redis | single node, default Compose caps |
| Outbox relay | `fixedDelay=1000ms` (polls the moderation outbox once per second) |

All latencies are **server-side**, measured after a warmup phase (JVM JIT / ONNX graph
optimization discarded), computed over the full request set (no sampling). Percentiles use
linear interpolation.

---

## A. Content-moderation ONNX inference latency

The `inference.score()` path (tokenize + ONNX run) exactly as the Kafka worker calls it,
over a corpus of realistic forum text (mixed short comments / medium / long 256-token posts).

| Batch | per-call p50 | p95 | p99 | max | per-**item** p50 | item p99 | throughput |
|------:|-----:|-----:|-----:|-----:|-----:|-----:|-----:|
| 1  | **5.8 ms** | 63.8 ms | 81.8 ms | 113.9 ms | 5.8 ms | 81.8 ms | 86 items/s |
| 8  | 121.6 ms | 579.2 ms | 660.7 ms | 816.7 ms | 15.2 ms | 82.6 ms | 29 items/s |
| 16 | 782.9 ms | 1044 ms | 1166 ms | 1277 ms | 48.9 ms | 72.9 ms | 24 items/s |
| 32 | 1589 ms | 1769 ms | 2049 ms | 2428 ms | 49.7 ms | 64.0 ms | 21 items/s |

**Reading it:** a single short comment scores in **~6 ms (p50)**; the p95/p99 at batch=1
(~64–82 ms) come entirely from the long-tail of 256-token posts. Throughput *falls* as batch
size rises because `score()` pads every item in a call to the longest item in that call — a
batch that contains one 256-token outlier does full-length work for all of them. (The worker
mitigates this with cross-batch length bucketing, but per-call padding still dominates on a
single CPU thread.) One single-threaded worker sustains **~85 short-item inferences/sec**,
already far above the Tier-1/Tier-2 inference-capacity targets (1–5/s).

## B. Post API latency — `POST /api/post/v1/post`

Measured **backend-direct** (loud generator on the internal network, forged `X-User-*`
headers — the backend trusts these; the gateway is the only thing that mints them). This is
the post API proper; it **excludes** gateway edge cost (RS256 verify + routing + rate-limit),
which would add a small fixed overhead. Each request does the real work: insert `posts` row +
moderation-outbox row in one transaction, forum `post_count` trigger fires.

| Load | p50 | p90 | p95 | p99 | max | mean |
|---|---:|---:|---:|---:|---:|---:|
| **Serial (c=1)** | **10.1 ms** | 23.2 ms | 31.1 ms | 40.8 ms | 100.5 ms | 13.5 ms |
| Concurrent c=8 | 54.8 ms | 138.6 ms | 184.8 ms | 389.8 ms | 931.5 ms | 71.9 ms |
| Concurrent c=32 | 186.4 ms | 503.3 ms | 659.0 ms | 913.0 ms | 1502 ms | 247.7 ms |

**Reading it:** the uncontended post-write path is **~10 ms (p50) / ~41 ms (p99)**. Under
c=32 the 2-vCPU backend saturates and latency climbs (p95 ≈ 660 ms) — expected for a single
small instance; this is where horizontal scaling would come in.

## C. End-to-end time-to-moderation-verdict (submit → verdict applied)

Time from the `POST` returning to the moment the post's `moderation_status` transitions out of
`PENDING` (APPROVED, or the post soft-removed if flagged), observed by polling Postgres.
Arrival ~6.7 posts/s, 25% deliberately toxic to exercise both verdict paths. Warm pipeline.

| n | p50 | p90 | p95 | p99 | max | mean |
|---|---:|---:|---:|---:|---:|---:|
| 120 | **724 ms** | 1149 ms | 1215 ms | 1376 ms | 1524 ms | 735 ms |

**Reading it:** end-to-end verdict latency is **~0.7 s (p50) / ~1.4 s (p99)**, dominated by
the 1-second outbox-relay poll (avg ~500 ms wait) plus Kafka hop + inference (~6–80 ms) +
verdict-consumer apply. This is well inside the aspirational target (p50 800 ms / p95 2 s /
p99 5 s) and — critically — is **fully decoupled** from the post API latency in (B): the user
gets their ~10 ms ack immediately; moderation happens asynchronously behind it.

> **Note on measurement:** an early version of the harness submitted all posts *before* it
> began polling, which lumped every verdict at the end of the submission window and produced a
> false ~9.6 s p50. Fixing it to poll *concurrently* with submission gave the ~0.7 s figure
> above. The pipeline itself was never slow (both Kafka consumer groups ran at zero lag).

---

## Reproducing

```bash
# Bring up the measurement subset (reads POSTGRES_* from .env — now consolidated)
docker compose up -d postgres kafka redis watch-party backend moderation-worker

# A — inference (runs inside the worker image, isolated from the pipeline)
docker run --rm --cpus=2 -v "$PWD/bench/bench_inference.py:/app/bench_inference.py:ro" \
  cinemate-moderation-worker:latest python -u /app/bench_inference.py

# B + C — post API + end-to-end verdict (container on the app network)
docker run --rm --network cinemate_app-net -e PGPASSWORD -e MODE=all \
  -v "$PWD/bench/bench_api.py:/bench_api.py:ro" python:3.11-slim \
  sh -c "pip install -q requests psycopg2-binary && python /bench_api.py"
```

> **Environment cleanup (done):** while measuring, the repo `.env` and `backend/.env.prod` were
> found still carrying the *pre-consolidation* MySQL/Mongo config (`DB_URL=jdbc:mysql://…`,
> `DB_USERNAME=root`, `MYSQL_*`, `MONGODB_*`) with **no** `POSTGRES_PASSWORD`, so `docker compose up`
> couldn't start Postgres. Both files were regenerated from their `*.env.example` templates
> (Postgres-only, stale vars removed, secrets preserved), so `docker compose up` now works with no
> manual overrides. `docker compose config` resolves cleanly.
