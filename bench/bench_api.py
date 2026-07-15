"""Post-API latency (B) + end-to-end time-to-moderation-verdict (C) benchmark.

Runs inside a container on the compose app-net. Hits the backend directly on
backend:8080 with forged X-User-* headers (the backend trusts these — the gateway
is the only thing that mints them, and the backend has no host port), so this
measures the post API proper, without gateway edge overhead. Verdict latency is
measured by polling Postgres for the moderation_status transition.
"""
import concurrent.futures as cf
import os
import random
import statistics
import time
import uuid

import psycopg2
import requests

BACKEND = os.environ.get("BACKEND", "http://backend:8080")
PG = dict(host="postgres", port=5432, dbname=os.environ.get("PGDB", "Cinemate"),
          user=os.environ.get("PGUSER", "cinemate"), password=os.environ["PGPASSWORD"])
POST_URL = f"{BACKEND}/api/post/v1/post"

random.seed(7)

CLEAN = [
    "great movie, the ending genuinely surprised me",
    "underrated performance, deserved way more awards buzz",
    "the pacing dragged in act two but the payoff was worth it",
    "cinematography carried this whole film honestly",
    "who else rewatched this over the weekend?",
    "the score is one of the best of the decade, no contest",
]
TOXIC = [
    "you are a complete idiot and everyone here hates you",
    "shut up you worthless piece of garbage nobody wants you here",
    "this is the most stupid moronic take i have ever seen you fool",
]

def make_text(toxic_ratio=0.25):
    return random.choice(TOXIC if random.random() < toxic_ratio else CLEAN)

def pctl(xs, p):
    xs = sorted(xs)
    if not xs:
        return 0.0
    k = (len(xs) - 1) * (p / 100.0)
    lo = int(k); hi = min(lo + 1, len(xs) - 1)
    return xs[lo] + (xs[hi] - xs[lo]) * (k - lo)

def summarize(name, ms):
    print(f"\n{name}  (n={len(ms)})")
    print(f"  p50={pctl(ms,50):.1f}  p90={pctl(ms,90):.1f}  p95={pctl(ms,95):.1f} "
          f"p99={pctl(ms,99):.1f}  max={max(ms):.1f}  mean={statistics.fmean(ms):.1f}  (ms)")

# ---------------------------------------------------------------- seed
def seed():
    conn = psycopg2.connect(**PG); conn.autocommit = True
    cur = conn.cursor()
    cur.execute("""
        INSERT INTO users (first_name,last_name,email,password,is_public,provider,profile_complete)
        VALUES ('Bench','User','bench@cinemate.local','x',true,'local',true)
        ON CONFLICT (email) DO UPDATE SET first_name=EXCLUDED.first_name
        RETURNING user_id;""")
    user_id = cur.fetchone()[0]
    cur.execute("SELECT id FROM forums WHERE name='bench-forum' LIMIT 1;")
    row = cur.fetchone()
    if row:
        forum_id = row[0]
    else:
        forum_id = str(uuid.uuid4())
        cur.execute("""INSERT INTO forums (id,owner_id,name,description,moderation_status)
                       VALUES (%s,%s,'bench-forum','benchmark forum','APPROVED');""",
                    (forum_id, user_id))
    cur.close(); conn.close()
    print(f"seeded user_id={user_id} forum_id={forum_id}")
    return user_id, str(forum_id)

def headers(user_id):
    return {"X-User-Id": str(user_id), "X-User-Role": "ROLE_USER",
            "X-User-Email": "bench@cinemate.local", "X-User-Name": "Bench User",
            "Content-Type": "application/json"}

def post_once(sess, hdrs, forum_id, toxic_ratio=0.0):
    body = {"forumId": forum_id, "title": "Benchmark post",
            "content": make_text(toxic_ratio)}
    t0 = time.perf_counter()
    r = sess.post(POST_URL, json=body, headers=hdrs, timeout=30)
    dt = (time.perf_counter() - t0) * 1000.0
    r.raise_for_status()
    return dt, r.text.strip()

# ---------------------------------------------------------------- B: post API latency
def bench_post_api(user_id, forum_id):
    hdrs = headers(user_id)
    sess = requests.Session()
    print("\n=== B: POST /api/post/v1/post latency (backend-direct) ===")
    # JVM warmup — first requests trigger class-load + JIT; discard them.
    for _ in range(200):
        post_once(sess, hdrs, forum_id)
    # Serial (concurrency=1): the pure per-request latency.
    serial = [post_once(sess, hdrs, forum_id)[0] for _ in range(400)]
    summarize("B1  serial (c=1)", serial)
    # Concurrent load.
    for c in (8, 32):
        def worker(_):
            s = requests.Session()
            return post_once(s, hdrs, forum_id)[0]
        n = 1500
        with cf.ThreadPoolExecutor(max_workers=c) as ex:
            lat = list(ex.map(worker, range(n)))
        summarize(f"B2  concurrent (c={c}, n={n})", lat)

# ---------------------------------------------------------------- C: time-to-verdict
def bench_verdict(user_id, forum_id, n=120, spacing_s=0.15):
    import threading
    hdrs = headers(user_id)
    sess = requests.Session()
    print("\n=== C: end-to-end time-to-moderation-verdict (submit -> status applied) ===")

    created = {}          # post_id -> t0 (perf_counter at submit)
    verdict_ms = {}       # post_id -> latency ms
    lock = threading.Lock()
    done_submitting = threading.Event()

    # Poller runs continuously IN PARALLEL with submission, so each verdict is
    # timestamped when it is actually applied — not lumped at the end of submission.
    def poller():
        conn = psycopg2.connect(**PG); conn.autocommit = True
        cur = conn.cursor()
        while True:
            with lock:
                pending = tuple(pid for pid in created if pid not in verdict_ms)
            if pending:
                cur.execute("""SELECT id::text, moderation_status, is_deleted
                               FROM posts WHERE id IN %s;""", (pending,))
                now = time.perf_counter()
                with lock:
                    for pid, status, is_deleted in cur.fetchall():
                        if (status != 'PENDING' or is_deleted) and pid not in verdict_ms:
                            verdict_ms[pid] = (now - created[pid]) * 1000.0
            if done_submitting.is_set():
                with lock:
                    remaining = [pid for pid in created if pid not in verdict_ms]
                if not remaining:
                    break
            time.sleep(0.02)
        cur.close(); conn.close()

    t = threading.Thread(target=poller, daemon=True); t.start()
    for _ in range(n):
        _, pid = post_once(sess, hdrs, forum_id, toxic_ratio=0.25)
        with lock:
            created[pid] = time.perf_counter()
        time.sleep(spacing_s)
    done_submitting.set()
    t.join(timeout=60)

    with lock:
        missing = [pid for pid in created if pid not in verdict_ms]
        ms = list(verdict_ms.values())
    if missing:
        print(f"WARNING: {len(missing)} posts never got a verdict within timeout")
    summarize("C  time-to-verdict (submit -> verdict applied)", ms)

def main():
    user_id, forum_id = seed()
    # health gate
    for _ in range(60):
        try:
            if requests.get(f"{BACKEND}/api/health", timeout=3).ok:
                break
        except Exception:
            pass
        time.sleep(2)
    mode = os.environ.get("MODE", "all")
    if mode in ("all", "b"):
        bench_post_api(user_id, forum_id)
    if mode in ("all", "c"):
        bench_verdict(user_id, forum_id)

if __name__ == "__main__":
    main()
