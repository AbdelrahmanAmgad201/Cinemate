"""Latency benchmark for the moderation ONNX inference path.

Calls the worker's own `inference.score()` (tokenize + ONNX run) exactly as the
Kafka worker does, over a corpus of realistic forum-text sizes. Reports per-call
and per-item latency percentiles and throughput, per batch size.

Run inside the built moderation-worker image so the model, tokenizer, deps, and
thread config are identical to production.
"""
import random
import statistics
import time

import inference  # the worker's module; loads model once at import

random.seed(1234)

# --- Realistic corpus: forum comments/posts. Sizes chosen to match typical content
# (comment ~150 B avg / 600 B P95, post body ~300 B avg / 1.2 KB P95).
# Mix of clean + toxic so both label paths run.
SHORT = [
    "great movie, loved the ending",
    "this take is so wrong lol",
    "who else rewatched this last night?",
    "the pacing dragged in act two honestly",
    "underrated performance, deserved an oscar",
    "you clearly have no idea what you're talking about",
    "nah the book was way better",
    "cinematography carried the whole thing",
]
MEDIUM = [
    "I think the third act completely undercuts the emotional setup from the first "
    "hour. The director clearly wanted a twist but it lands flat because we were "
    "never given a reason to trust the narrator in the first place.",
    "Hot take: the sequel is better than the original. The world-building finally "
    "pays off, the side characters get real arcs, and the score is genuinely one of "
    "the best of the decade. Fight me in the replies.",
    "honestly this forum has gone downhill, half the posts are just people being "
    "obnoxious and picking fights instead of actually discussing the films",
]
LONG = [
    "Long-form review incoming. " + ("This film rewards patience in ways modern "
    "blockbusters rarely attempt; every frame is composed with intent, and the "
    "restraint in the first ninety minutes is precisely what makes the final "
    "sequence detonate. ") * 6,
]

def make_text():
    r = random.random()
    if r < 0.60:
        return random.choice(SHORT)
    if r < 0.92:
        return random.choice(MEDIUM)
    return random.choice(LONG)

def make_batch(n):
    return [make_text() for _ in range(n)]

def pctl(xs, p):
    xs = sorted(xs)
    if not xs:
        return 0.0
    k = (len(xs) - 1) * (p / 100.0)
    lo = int(k)
    hi = min(lo + 1, len(xs) - 1)
    return xs[lo] + (xs[hi] - xs[lo]) * (k - lo)

def bench(batch_size, iters, warmup):
    # Warmup: first ONNX runs trigger graph optimization + allocation.
    for _ in range(warmup):
        inference.score(make_batch(batch_size))
    call_ms = []
    for _ in range(iters):
        b = make_batch(batch_size)
        t0 = time.perf_counter()
        inference.score(b)
        call_ms.append((time.perf_counter() - t0) * 1000.0)
    total_items = batch_size * iters
    total_s = sum(call_ms) / 1000.0
    return {
        "batch": batch_size,
        "iters": iters,
        "call_p50": pctl(call_ms, 50),
        "call_p90": pctl(call_ms, 90),
        "call_p95": pctl(call_ms, 95),
        "call_p99": pctl(call_ms, 99),
        "call_max": max(call_ms),
        "call_mean": statistics.fmean(call_ms),
        "item_p50": pctl(call_ms, 50) / batch_size,
        "item_p95": pctl(call_ms, 95) / batch_size,
        "item_p99": pctl(call_ms, 99) / batch_size,
        "throughput": total_items / total_s,
    }

def main():
    print(f"ONNX providers: {inference._session.get_providers()}")
    print(f"intra_op_threads={inference.INTRA_OP_THREADS} inter_op_threads={inference.INTER_OP_THREADS} "
          f"max_seq_len={inference.MAX_SEQ_LEN}")
    print(f"model file: {inference._onnx_file}\n")

    configs = [
        (1, 300, 20),
        (8, 200, 10),
        (16, 200, 10),
        (32, 150, 10),
    ]
    rows = [bench(bs, it, wu) for bs, it, wu in configs]

    print(f"{'batch':>5} {'iters':>6} | per-CALL latency (ms)                    | per-ITEM (ms)          | items/s")
    print(f"{'':>5} {'':>6} | {'p50':>7} {'p90':>7} {'p95':>7} {'p99':>7} {'max':>7} {'mean':>7} | {'p50':>6} {'p95':>6} {'p99':>6} |")
    print("-" * 108)
    for r in rows:
        print(f"{r['batch']:>5} {r['iters']:>6} | "
              f"{r['call_p50']:>7.2f} {r['call_p90']:>7.2f} {r['call_p95']:>7.2f} "
              f"{r['call_p99']:>7.2f} {r['call_max']:>7.2f} {r['call_mean']:>7.2f} | "
              f"{r['item_p50']:>6.2f} {r['item_p95']:>6.2f} {r['item_p99']:>6.2f} | "
              f"{r['throughput']:>7.0f}")

if __name__ == "__main__":
    main()
