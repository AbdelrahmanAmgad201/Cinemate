# Cinemate — Engineering Target Metrics & Capacity Specification

**Document type:** Internal engineering performance specification (SLOs, performance budgets, capacity plan)
**Status:** Target state — defines what a production-grade version of this system must achieve. Does not describe the current implementation and does not prescribe how targets should be met.
**Audience:** The engineer(s) responsible for redesigning, benchmarking, and evolving Cinemate toward these targets.

> This document defines **success criteria**, not solutions. It specifies what must be true of the system — measured, numeric, testable — at increasing levels of scale. It does not recommend architectures, technologies, or optimizations. Every number is derived from an explicit, stated assumption about user behavior or system scale; none are arbitrary. Where an assumption is a judgment call rather than a hard fact, it is labeled as such.

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Workload Assumptions](#2-workload-assumptions)
3. [Engineering Target Metrics — Framework & Notation](#3-engineering-target-metrics--framework--notation)
4. [Feature-Specific Targets](#4-feature-specific-targets)
5. [Infrastructure Targets](#5-infrastructure-targets)
6. [Database Targets](#6-database-targets)
7. [Performance Targets](#7-performance-targets)
8. [Scalability Tiers](#8-scalability-tiers)
9. [Benchmarking Methodology](#9-benchmarking-methodology)
10. [Closing Notes](#10-closing-notes)

---

## 1. Executive Summary

Cinemate is a polyglot-persistence social platform for movie discovery, forum-style discussion, and synchronized real-time watch parties. Its feature surface spans nine distinct workload classes, each with a different read/write ratio, latency sensitivity, and failure blast radius: authentication, movie catalog browsing/search, social feed (forums/posts), comments, voting, content moderation, real-time watch parties (WebSocket fan-out), user/social-graph management, and admin/organization operations.

This document exists because "make it fast" and "make it scale" are not engineering targets — they are aspirations. An engineering target is a number, a unit, a measurement method, and a justification. This document supplies all four for every major subsystem, at four progressively harder scale tiers, so that "done" has an unambiguous, falsifiable definition at every stage of the system's evolution.

**The central design principle of this specification:** *latency and correctness targets do not relax as scale increases.* A user's experience of "how fast did my vote register" should be identical whether the platform has 800 daily active users or 4 million. What *does* change across tiers is the throughput, concurrency, and fan-out load the system must sustain while holding that latency bar — and it is specifically the tension between "the SLO doesn't move" and "the load keeps growing" that makes this exercise a system-design problem rather than a tuning exercise. A handful of targets (real-time fan-out tail latency at extreme concurrency, cross-region network floors) are explicitly and separately called out as tier-specific where physics, not architecture quality, is the limiting factor — this is standard practice in real capacity-planning documents and is not the same thing as "loosening the bar because it got hard."

Ninety-one findings from the prior engineering audit (`FINAL_AUDIT.md`) describe *where the current implementation falls short of correctness and production-readiness*. This document is deliberately independent of that audit: it does not reference specific code, files, or bugs. It answers a different question — not "what is wrong with the code," but "what would have to be true, numerically, for this system to be considered production-grade at a given scale." The two documents are complementary: the audit is a diagnosis: this document is a target state to design toward, at whatever pace and in whatever order the owner chooses.

---

## 2. Workload Assumptions

Every throughput number in this document is derived from the same small set of behavioral assumptions, applied consistently across every feature. This section states them once, with justification, so that every downstream number is traceable back to a stated premise rather than invented per-metric.

### 2.1 Population assumptions (engagement funnel)

| Assumption | Value | Justification |
|---|---|---|
| MAU / Registered Users | 35–40% | Typical for a social product with organic + retained growth; consistent with published engagement ratios for mid-size social apps (higher than utility apps, lower than daily-habit apps like messaging). |
| DAU / MAU (stickiness) | 20–22%, rising slightly with scale | Standard "DAU/MAU ratio" industry benchmark for social/content platforms is 15–25%; the upper end is used here because watch parties are a synchronous, appointment-driven feature that increases return frequency as the social graph densifies. |
| Peak Concurrent Users (PCU) / DAU | 15% at small/medium scale, falling to 10% at internet scale | Consumer apps concentrate usage in an evening "prime time" window; at small scale this concentration is sharp (single time zone, single content calendar). At internet scale, traffic smooths across time zones, which *lowers* the peak-to-average ratio even as absolute peak grows enormously. This is a real, well-documented effect of global scale, not an assumption invented to make Tier 4 easier — Tier 4 is still by far the hardest tier in absolute numbers. |

### 2.2 Session behavior assumptions

| Assumption | Value | Justification |
|---|---|---|
| Sessions per DAU per day | 3 | Typical for a "check-in" social app (commute, lunch, evening), consistent with the login/re-auth pattern implied by a stateless token with sub-day-length practical reuse. |
| Average session length | 12–15 minutes | Mid-range for content-browsing + occasional real-time (watch party) sessions; shorter than pure video-streaming, longer than a utility app. |
| Screens/feed-pages viewed per session | 10 | Reflects an infinite-scroll browsing pattern across movie catalog and forum feed combined. |
| Split of the 10 screens/session between Movie Catalog (MySQL-backed) and Forum/Post Feed (MongoDB-backed) | 40% / 60% | Movie discovery is a secondary, less frequent activity relative to the higher-frequency, lower-effort act of scrolling a social feed; this split is used consistently wherever a datastore-specific read load must be estimated. |

### 2.3 Engagement funnel (participation inequality)

Every "write" action is modeled as a fixed conversion percentage of "read" actions (feed/catalog views), following the well-documented **90-9-1 rule of online participation** (≈90% of users only consume, ≈9% contribute lightly, ≈1% create): each successive tier of effort (voting → commenting → posting) is modeled as an order of magnitude rarer than the one before it, since each requires progressively more user effort (one tap vs. typing vs. composing).

| Action | Conversion rate from a feed/catalog view | Justification |
|---|---|---|
| Vote (upvote/downvote) | 8% | Lowest-friction possible action (single tap); sits at the upper end of the "9% contributor" tier. |
| Comment | 1.2% | Requires composing text; an order of magnitude below voting. |
| New post | 0.1% | Requires the most effort (title + body, occasionally media); consistent with the "1% creator" tier, further discounted because posting also requires forum context. |
| Search/filter query | 15% | Modeled independently — browsing sessions commonly *begin with* or *include* a search/filter action, so this is not a "conversion" of a prior view but a distinct action type that co-occurs with browsing. |
| Follow/unfollow (user or forum) | 0.05% | Rarer than posting — a durable relationship decision, made occasionally, not per-session. |
| Watch-party adoption (fraction of concurrent users who are inside a watch party at any given peak moment) | 15% (Tier 1) → 30% (Tier 4), rising with tier | Modeled as *rising* with scale because watch parties are a network-effect feature: their value (finding people to watch with) increases as the user base grows, which is a defensible assumption specifically because it makes higher tiers harder, not easier. |

### 2.4 Content size assumptions

| Content type | Assumption | Justification |
|---|---|---|
| Post title | ~60 bytes average | Short-form heading text. |
| Post body (text-only, ~70% of posts) | 300 bytes average, 1.2 KB P95 | Forum/Reddit-style discussion post length distribution — long tail of longer posts. |
| Post with attached media (~30% of posts) | +150 KB average delivered asset, +2 MB P95 source upload | Modeled as an image attachment; video attachments are out of scope for this specification (watch parties reference externally-hosted video, they do not host video). |
| Comment | 150 bytes average, 600 bytes P95 | Shorter than posts; conversational reply length. |
| Movie catalog record | ~2 KB (metadata + poster reference, not the poster binary itself) | Structured metadata record. |
| Vote record | <100 bytes | Minimal state: actor, target, direction. |
| Watch-party control event | <200 bytes | Playback position + action type, minimal payload. |
| Chat message | ~120 bytes average | Short-form real-time text. |

### 2.5 Peak concentration factors

Consumer social traffic is diurnal, not flat. Rather than inventing a single "peak multiplier," this document uses feature-appropriate concentration factors, each justified by the nature of the feature:

| Feature class | Peak / Average multiplier | Justification |
|---|---|---|
| General browsing/reads (feed, catalog, search, profile) | 6× | Standard capacity-planning convention for evening-prime-time-weighted consumer traffic (traffic concentrates in a 3–4 hour evening window). |
| Writes correlated with browsing (votes, comments, posts) | 6× (same curve as the reads that drive them) | These actions are a fixed conversion of browsing traffic, so they inherit the same peak shape. |
| Authentication (logins) | 8× | Login events cluster more sharply than general browsing — a large fraction of DAU authenticate within the first 30–60 minutes of the prime-time window rather than spreading evenly across it. |
| Watch parties (concurrency, control events, chat) | 8–10× baseline, with a separately-modeled "flagship event" stretch scenario | Watch parties are appointment-driven (a movie starts at a specific time), producing sharper concentration than passive browsing; flagship/premiere events are modeled as an explicit additional stress scenario, not folded into the routine peak. |

### 2.6 Tier population summary (see §8 for full detail and rationale)

| | Tier 1 — Small | Tier 2 — Growing | Tier 3 — Large-Scale | Tier 4 — Internet-Scale |
|---|---|---|---|---|
| Registered users | 10,000 | 250,000 | 5,000,000 | 50,000,000 |
| MAU | 4,000 | 90,000 | 1,750,000 | 18,000,000 |
| DAU | 800 | 18,000 | 350,000 | 4,000,000 |
| Peak Concurrent Users (PCU) | 120 | 2,700 | 52,500 | 400,000 |

All feature-level throughput figures in §4 are derived from this table using the ratios in §2.1–§2.5. Figures are rounded to 2 significant figures — this is a capacity-planning document, not an exact accounting ledger; the methodology, not decimal precision, is what must be reproducible.

---

## 3. Engineering Target Metrics — Framework & Notation

### 3.1 SLI / SLO / SLA definitions used in this document

- **SLI (Service Level Indicator):** a directly measured quantity (e.g., "P95 latency of `POST /vote`").
- **SLO (Service Level Objective):** a target value for an SLI over a defined measurement window (e.g., "P95 latency of `POST /vote` ≤ 120 ms, measured over any rolling 5-minute window, at the Tier-appropriate peak load"). Every numeric target in §4–§7 is an SLO in this sense.
- **SLA (Service Level Agreement):** an externally-facing commitment with consequences for breach. This document does not define SLAs — it defines the internal engineering bar that would need to be cleared before any external SLA could responsibly be offered.
- **Error budget:** `1 − Availability Target`, expressed as allowable downtime/error-rate over a rolling period. Defined explicitly per tier in §5.

### 3.2 Percentile methodology

Unless stated otherwise, all P50/P95/P99 figures in this document:

- Are measured **server-side**, at the boundary of the service under test (i.e., time from request received to response sent), excluding client network transit time unless a metric is explicitly labeled "end-to-end" or "client-observed."
- Are computed over a **rolling 5-minute window** during sustained load, and separately over the **single busiest 1-minute window** during a peak/burst test — both figures should be reported, since a system can pass on a 5-minute average while still violating the target during its worst minute.
- Are computed from **all requests to the endpoint/operation**, not a sampled subset, unless request volume makes full capture infeasible (see §9.4).

### 3.3 Notation used throughout this document

| Symbol / Term | Meaning |
|---|---|
| T1 / T2 / T3 / T4 | Scalability Tier 1 through 4 (§8) |
| RPS | Requests per second |
| QPS | Queries per second (database-level, may be >1 query per request) |
| PCU | Peak Concurrent Users |
| DAU / MAU | Daily / Monthly Active Users |
| Fan-out delivery | One message broadcast to N recipients counts as N deliveries for load-modeling purposes, since each delivery consumes real network/serialization work |
| "Target" vs. "Stretch target" | A **target** is the bar the system must clear to be considered production-grade at that tier. A **stretch target** is an explicitly harder, lower-frequency scenario (e.g., a single mega-event) that the system must survive without violating its availability SLO, even if steady-state targets are temporarily relaxed for that specific traffic class. |

---

## 4. Feature-Specific Targets

Every subsection below states: the workload derivation (peak throughput per tier, from §2's formulas), the latency SLOs (which — per the design principle in §1 — hold constant across tiers unless explicitly noted), and why the metric matters. All throughput figures are **peak** (i.e., already include the concentration factor from §2.5) unless labeled "average."

### 4.1 Authentication & Session Management

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Login throughput (peak) | 0.1 | 2.2 | 42 | 480 | logins/sec |
| Token validation throughput (peak) | 3 | 60 | 1,100 | 12,600 | validations/sec |
| Concurrently active authorization contexts | 120 | 2,700 | 52,500 | 400,000 | identities |

**Latency targets (constant across all tiers):**

| Operation | P50 | P95 | P99 | Why |
|---|---|---|---|---|
| Login (credentials or OAuth callback → issued token) | 150 ms | 400 ms | 800 ms | Includes deliberately expensive password verification; still must feel instant to a human. |
| Token validation (per authenticated request) | 5 ms | 15 ms | 40 ms | Executes on **every** API call in the system — it is the multiplier on every other feature's latency budget. A slow auth check inflates every other SLO in this document. |

**Why token validation throughput is the load-bearing metric:** it must sustain the platform's *entire* aggregate authenticated request rate (§4.10), not just login events. A system that meets every other target in this document but bottlenecks on authorization checks has not actually met any of them, because every one of those other requests passes through this check first.

**Measurement approach:** synthetic credential-stuffing-shaped load test (valid random users, no think-time) against the login endpoint; separately, a saturating load test against any authenticated endpoint with token validation instrumented as a standalone server-side span, isolated from downstream business logic.

---

### 4.2 Movie Catalog (Browse & Search)

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Catalog reads (browse/detail view, peak) | 0.7 | 16 | 290 | 3,300 | reads/sec |
| Search/filter queries (peak) | 0.25 | 5.6 | 110 | 1,250 | queries/sec |
| Catalog corpus size | 500 | 5,000 | 50,000 | 300,000 | distinct titles |
| Catalog write rate (new/updated titles, org submissions approved) | 1.7 | 27 | 400 | 2,680 | titles/day |

**Latency targets (constant across all tiers):**

| Operation | P50 | P95 | P99 | Why |
|---|---|---|---|---|
| Catalog browse / filtered search | 80 ms | 250 ms | 600 ms | Read-heavy, public, unauthenticated-reachable in many cases — the platform's front door. |
| Movie detail view | 60 ms | 180 ms | 400 ms | Single-record lookup; should be near-instant regardless of catalog size. |
| Index freshness (submission approved → visible in search results) | — | ≤ 5 s | ≤ 30 s | Not a request-latency metric — a data-propagation metric. Catalog changes are not urgent the way social content is, but "approved and still invisible an hour later" is a defect. |

**Why catalog latency must be scale-invariant:** search/filter latency is a function of *corpus size* (500 → 300,000 titles across tiers), not user count — this is the one feature area where the hard part is genuinely a data-scale problem (index growth) rather than a concurrency problem, and it is exactly why the P95/P99 targets above must hold even as the corpus grows 600×.

**Measurement approach:** load test with query patterns drawn from a realistic filter-value distribution (Zipfian — a small number of genres/titles receive most queries, matching real search-term distributions), not uniform random filters, since uniform random queries are unrealistically cache-hostile and will produce misleadingly pessimistic results.

---

### 4.3 Social Feed & Forums (Posts)

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Feed/forum reads (peak) | 1.0 | 24 | 440 | 5,000 | reads/sec |
| Post creation (peak) | 0.002 | 0.04 | 0.73 | 8.3 | posts/sec |
| Post creation (daily total) | 24 | 540 | 10,500 | 120,000 | posts/day |
| Average post size | 300 B (1.2 KB P95) text; +150 KB (P95 2 MB) when media attached (~30% of posts) | | | | payload |
| Read : write ratio | ≈500 : 1 | | | | — |

**Latency targets (constant across all tiers):**

| Operation | P50 | P95 | P99 | Why |
|---|---|---|---|---|
| Feed / forum page read (paginated) | 100 ms | 300 ms | 700 ms | Core browsing loop; this is the endpoint the user hits most often, by volume. |
| Post creation — user-facing acknowledgment | 150 ms | 400 ms | 900 ms | This is the "did my post go through" latency the user perceives. It is explicitly **decoupled** from moderation-decision latency (§4.6) — a production system must not make the user wait for a moderation verdict to get an ack. |
| Post creation — fully durable & consistent in feed | ≤ 2 s (P95) | | | Time until the new post is reliably visible to *other* users browsing the same feed, accounting for any propagation/consistency delay. |

**Why the 500:1 read:write ratio matters:** it means the system's read path — caching, query patterns, pagination — carries essentially all of the engineering risk for this feature; the write path, while lower-volume, carries the correctness risk (a lost or duplicated post is worse than a slow read).

---

### 4.4 Comments & Nested Replies

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Comment creation (peak) | 0.02 | 0.45 | 8.7 | 100 | comments/sec |
| Comment creation (daily total) | 290 | 6,500 | 126,000 | 1,440,000 | comments/day |
| Comment thread reads (bundled with post reads, §4.3) | — | — | — | — | included above |
| Average comment size | 150 B (600 B P95) | | | | payload |
| Maximum supported nesting depth under load | ≥ 8 levels | | | | correctness bound, not a perf number, but must not degrade with depth |

**Latency targets (constant across all tiers):**

| Operation | P50 | P95 | P99 | Why |
|---|---|---|---|---|
| Comment creation — user-facing ack | 100 ms | 300 ms | 600 ms | Faster budget than posts (§4.3) — comments are the higher-frequency, lower-effort action and users expect near-immediate feedback. |
| Comment count / reply count consistency | ≤ 1 s (P95) to reflect in parent counters | | | Denormalized counters (post comment-count, parent reply-count) must converge quickly and **must never go negative or diverge permanently** — this is a correctness SLO as much as a latency one. |

**Why comment volume is treated separately from posts:** comments outnumber posts by roughly an order of magnitude at every tier (matching the engagement-funnel assumption in §2.3), and unlike posts, comments form a recursive structure (replies to replies) — the correctness bound on nesting depth and counter convergence is as important as the throughput number.

---

### 4.5 Voting (Upvote / Downvote)

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Vote actions (peak) | 0.13 | 3.0 | 58 | 670 | votes/sec |
| Vote actions (daily total) | 1,940 | 43,000 | 840,000 | 9,600,000 | votes/day |
| Concurrent votes on a single hot post/comment (worst case, viral content) | 5 | 40 | 400 | 3,000 | simultaneous writers to one document |

**Latency targets (constant across all tiers):**

| Operation | P50 | P95 | P99 | Why |
|---|---|---|---|---|
| Vote registration — user-facing ack | 50 ms | 120 ms | 250 ms | This is the single lowest-friction, highest-frequency write in the system — it must feel instantaneous or it will visibly lag the UI interaction (a button press with no immediate feedback). |
| Vote count correctness under concurrency | **Zero tolerance for lost updates** | | | Not a latency target — a correctness target. At the "concurrent votes on a single hot post" row above, every single vote must be reflected in the final count with no losses, regardless of timing overlap. This is the hardest correctness bar in this document because it is the easiest metric to *appear* to pass in a single-user test and silently fail under real concurrency. |

**Why the "concurrent votes on one hot post" row is the real target:** aggregate votes/sec across the whole platform is a relatively easy number to hit. The number that actually tests the system is: what happens when a single piece of viral content receives hundreds or thousands of simultaneous votes on the *same underlying counter*, at Tier 3–4 scale. This is a hot-key/contention problem, not a throughput problem, and the two require fundamentally different validation approaches (§9).

---

### 4.6 Content Moderation (Hate-Speech Detection)

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Content items requiring moderation (peak) — all posts + all comments | 0.022 | 0.49 | 9.4 | 108 | items/sec |
| Content items requiring moderation (daily total) | 2,200 | 49,000 | 940,000 | 10,800,000 | items/day |

At production maturity, **100% of user-generated text content** (posts, comments, and forum metadata) requires a moderation verdict before or shortly after publication — partial coverage (e.g., only checking one content type) is a correctness gap, not an acceptable steady state.

**Latency targets (constant across all tiers):**

| Operation | P50 | P95 | P99 | Why |
|---|---|---|---|---|
| User-facing write acknowledgment (post/comment accepted by the system) | *(see §4.3 / §4.4 — must not be inflated by moderation)* | | | The user's perceived latency for creating content must be independent of how long moderation takes to reach a verdict. |
| Time-to-moderation-verdict (submission → verdict available) | 800 ms | 2 s | 5 s | This is the time until the system knows whether content is acceptable — independent of when/whether that verdict gates visibility, which is a design decision outside this document's scope. |
| Moderation model quality — false-negative rate (hate speech missed) | ≤ 2% | | | | A quality SLO, not a performance one — but it is a measurable, production-relevant target for any ML-backed safety system and belongs alongside the latency/throughput numbers. |
| Moderation model quality — false-positive rate (safe content wrongly flagged) | ≤ 1% | | | | Over-flagging directly harms legitimate users and erodes trust in the platform; it is graded as a stricter bound than the false-negative rate because it has a more direct, visible user-facing cost. |

**Inference capacity target (the moderation service itself):**

| | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Sustained inference throughput required | 1 | 5 | 50 | 200 | inferences/sec |

*(Modeled with headroom above the raw content-volume figures above, since re-checks, retries, and title+body as separate units of work all add inference load beyond the raw per-item rate.)*

**Why this is treated as its own feature area:** moderation sits on the critical path of every single piece of user-generated content in the system, but its performance characteristics (ML inference cost, quality trade-offs) are fundamentally different from the request/response latency of the rest of the API — it deserves independent measurement rather than being folded into "post creation latency."

---

### 4.7 Watch Parties (Real-Time Synchronization + Chat)

This is the single hardest feature area in this specification, because it is the only one where server load scales with **fan-out** (one action → N deliveries) rather than linearly with request count. It receives the most detailed treatment accordingly.

**4.7.1 Concurrency**

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Concurrent watch parties (steady-state peak) | 2 | 68 | 1,640 | 15,000 | parties |
| Average party size | 8 | 8 | 8 | 8 | users/party |
| Users concurrently in a watch party (steady-state peak) | 18 | 540 | 13,100 | 120,000 | users |
| **Flagship/premiere event — single party size (stretch target)** | 25 | 300 | 5,000 | **50,000** | users in *one* party |

The flagship-event row is the genuine engineering challenge in this feature area: synchronizing playback across 50,000 simultaneous participants in a single logical "room" is a materially different problem from synchronizing 8, and is deliberately included as an explicit, separately-graded stretch target rather than averaged away into the steady-state numbers.

**4.7.2 Control-plane throughput (playback control: play/pause/seek)**

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Host control actions generated (peak, across all parties) | 0.003 | 0.11 | 2.7 | 25 | events/sec |
| Resulting fan-out deliveries (steady-state parties, event × avg party size) | 0.03 | 0.9 | 22 | 200 | deliveries/sec |
| Resulting fan-out deliveries from **one** control action in the flagship-event party | 25 | 300 | 5,000 | **50,000** | deliveries, single event |

**4.7.3 Chat throughput**

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Chat messages (steady-state parties, peak) | 0.13 | 4.5 | 109 | 1,000 | messages/sec |
| Chat messages in the flagship-event party (peak, ~5% concurrent active-chatter rate at extreme room size) | 0.02 | 0.25 | 4.2 | 42 | messages/sec, single room |

**4.7.4 Latency & synchronization targets**

| Operation | Target | Applies to |
|---|---|---|
| Control-action propagation latency (host action → all recipients receive it) | P50 ≤ 100 ms, P95 ≤ 300 ms, P99 ≤ 600 ms | Steady-state parties (≤ ~20 users) |
| Control-action propagation latency — flagship event | P50 ≤ 250 ms, P95 ≤ 700 ms, **P99 ≤ 1.5 s** | Explicitly relaxed P99 only, justified below |
| Cross-client playback position drift (at any moment, any two clients in the same party) | P95 ≤ 300 ms, P99 ≤ 800 ms | All party sizes |
| Chat message delivery latency | P50 ≤ 150 ms, P95 ≤ 400 ms, P99 ≤ 900 ms | All party sizes |
| Party join latency (request to join → fully synchronized state received) | P50 ≤ 500 ms, P95 ≤ 1.5 s, P99 ≤ 3 s | All party sizes |
| Party creation → Redis/state-store initialization → ready-to-join | P95 ≤ 1 s | All tiers |

**Why the flagship-event P99 is the one explicitly relaxed number in this entire document:** delivering a message to 50,000 concurrent recipients has an irreducible serialization/network-fan-out cost that does not exist at 8 recipients — this is a physical property of fan-out, not a quality shortfall. Relaxing *only the P99 tail*, and *only* for this one named scenario, while holding P50/P95 to a much tighter bar, is standard practice for large-scale real-time systems (comparable to how live-streaming chat platforms grade "message seen by 95% of viewers within X ms" separately from worst-case tail behavior at extreme concurrency). Every other number in this document holds constant across tiers by design; this is the sole, deliberate, justified exception.

**Measurement approach:** synchronization drift and fan-out latency cannot be measured via simple HTTP load testing — they require a purpose-built harness that spins up N simulated WebSocket clients in a single logical room, has one "host" client issue a control event, and measures receipt timestamps across all N clients (see §9.3 for the full methodology). Steady-state and flagship-event scenarios must be tested as **separate, explicitly labeled test runs**, not extrapolated from each other.

---

### 4.8 User Profile & Social Graph (Follows, Liked Movies, Watch History/Later, Reviews)

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Profile reads (own + others', peak) | 0.33 | 8 | 150 | 1,700 | reads/sec |
| Follow/unfollow actions (peak) | 0.001 | 0.02 | 0.37 | 4.2 | actions/sec |
| Social-graph writes — liked movies, watch history, watch-later (peak, combined) | 0.017 | 0.4 | 7.5 | 85 | writes/sec |
| Movie review submissions (peak, approximated at ~3× the post-creation rate — reviews require less effort than a full forum post) | 0.005 | 0.11 | 2.2 | 25 | reviews/sec |

**Latency targets (constant across all tiers):**

| Operation | P50 | P95 | P99 | Why |
|---|---|---|---|---|
| Profile page load (own or others') | 100 ms | 300 ms | 650 ms | Aggregates several sub-reads (basic info, review count, follower count) — must not become slower than a simple single-table read as those sub-reads are added. |
| Follow/unfollow action | 80 ms | 200 ms | 450 ms | Low-frequency but must feel immediate — this is a relationship-defining action a user performs deliberately and expects instant confirmation of. |
| Follower/following count consistency | Same correctness bar as §4.4's comment counters — must never drift or go negative | | | |
| Watch history / liked-movies / watch-later write | 80 ms | 200 ms | 450 ms | Simple, low-contention writes; should be among the fastest operations in the system. |
| Movie review submission (includes recomputing the movie's aggregate rating) | 150 ms | 400 ms | 900 ms | Slightly higher budget than a simple write because it touches a shared, contended aggregate (the movie's average rating) rather than a private record. |

**Why the follower-graph write path deserves its own correctness bar:** unlike vote counts (§4.5), which are numeric aggregates, the social graph is a *relationship* — a lost or duplicated "follow" write corrupts not just a counter but the actual graph structure other features (feed ranking, notifications) depend on.

---

### 4.9 Admin & Organization Operations

Unlike every other feature area, admin/organization workload does not scale with DAU — it scales with the number of organizations and admins on the platform, which is a much smaller, slower-growing population.

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Organizations on platform | 25 | 400 | 6,000 | 40,000 | orgs |
| Admin accounts | 3 | 15 | 100 | 500 | admins |
| Movie submission requests | 1.7 | 27 | 400 | 2,680 | requests/day |
| Admin/org dashboard aggregate queries (system overview, analytics) | 0.01 | 0.1 | 1.5 | 8 | queries/sec, peak |

**Latency targets (constant across all tiers):**

| Operation | P50 | P95 | P99 | Why |
|---|---|---|---|---|
| Movie submission / approval action | 200 ms | 500 ms | 1.2 s | Workflow action, not a hot-path read — a slightly larger budget than user-facing actions is acceptable. |
| Admin/org dashboard aggregation query (analytics, system overview) | 300 ms | 800 ms | 2 s | Heavier analytical queries; acceptable to be slower than hot-path features, but still bounded — an admin dashboard that times out during an incident is actively harmful. |
| **Any list-returning admin/org endpoint — response bound regardless of underlying record count** | — | **≤ 500 KB response payload, P99 ≤ 1.5 s, regardless of total matching record count** | | This is the target that matters most in this section: as organizations and requests grow from 25/1.7-per-day to 40,000/2,680-per-day, an endpoint that returns "all matching records" in one response degrades linearly with data growth, which is precisely the kind of scale-sensitivity this entire document exists to catch. The target is stated as a hard payload/latency ceiling *independent of record count* — how that invariant is achieved is not in scope here. |

---

### 4.10 Rollup: Total Platform API Throughput

| | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| **Total authenticated API requests (peak, sum of all HTTP-facing features above)** | **~3** | **~60** | **~1,100** | **~12,600** | RPS |
| Total WebSocket fan-out deliveries (peak, watch-party control + chat, steady-state parties only) | ~0.16 | ~5.4 | ~131 | ~1,200 | deliveries/sec |
| Total WebSocket fan-out deliveries — single flagship event (stretch, one control action) | 25 | 300 | 5,000 | 50,000 | deliveries, one event |

This rollup is the number referenced by §4.1's token-validation-throughput target, §5's infrastructure sizing, and §6's database connection/QPS targets — it is the single figure that determines whether the system as a whole, not any one feature in isolation, is production-ready at a given tier.

---

## 5. Infrastructure Targets

| Metric | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Maximum concurrent users supported | 120 | 2,700 | 52,500 | 400,000 | PCU |
| Sustained (average) request rate | 0.4 | 9.4 | 180 | 2,100 | RPS |
| Peak request rate | 3 | 60 | 1,100 | 12,600 | RPS |
| WebSocket fan-out deliveries, steady-state peak | 0.16 | 5.4 | 131 | 1,200 | deliveries/sec |
| WebSocket fan-out deliveries, flagship-event stretch | 25 | 300 | 5,000 | 50,000 | deliveries/single event |

**Resource utilization targets (apply at every tier, at that tier's peak load):**

| Metric | Target | Why |
|---|---|---|
| CPU utilization per compute instance, sustained peak | ≤ 70% | Preserves ≥30% headroom for autoscaling reaction lag and traffic mis-estimation; a system running at 95%+ CPU at "normal peak" has no margin for the inevitable day it's wrong about demand. |
| Memory utilization per instance, sustained peak | ≤ 80% | Slightly higher tolerance than CPU since memory pressure degrades gracefully (GC pressure, page cache eviction) before it becomes catastrophic (OOM), but must not approach the OOM boundary under rated load. |
| Zero OOM-kills / forced restarts | Over a 7-day continuous soak test at Tier-appropriate sustained load | Distinguishes "fits in memory today" from "doesn't leak and remains stable over time" — a materially harder and more relevant bar. |
| Network bandwidth — API traffic (assumes ~5 KB average JSON response payload) | 0.12 Mbps (T1) → 504 Mbps (T4) | Derived directly from peak RPS × average response size; becomes a real capacity-planning input only at T3–T4. |
| Network bandwidth — media/static-asset delivery (image attachments, ~30% of viewed posts, ~150 KB delivered size) | negligible (T1) → **~1.8 Gbps (T4)** | Derived from feed-read volume × media-attachment rate × delivered-asset size; at T4 this exceeds API bandwidth by more than 3×, making media delivery — not the API — the dominant bandwidth consumer at scale. |
| Storage growth (structured data + retained media, blended) | ~15 MB/day (T1) → **~75 GB/day (T4)** | Derived from daily content volume (§4.3/§4.4) × content size assumptions (§2.4); at T4, growth is media-dominated (occasional large image uploads outweigh the far more numerous but tiny text records). |
| Horizontal scaling efficiency | Adding the Nth stateless compute instance must yield ≥ 90% of the marginal throughput gain of instance N−1 (≤ 10% coordination-overhead loss per doubling), up to at least 32 concurrent instances at T4 | Defines "scales horizontally" as a measurable curve (throughput vs. instance count), not an assertion — a system that plateaus at 8 instances has *not* met a horizontal-scalability target even if it "has more than one instance." |
| Database connections in use, sustained peak | See §6.4 (Little's-Law-derived target, computed from observed query concurrency, not a fixed guess) | |
| Async/background queue backlog depth, sustained peak | ≤ 2 × the steady-state arrival rate's worth of items (i.e., the queue should drain in well under one steady-state "tick" even during a peak burst) | A queue that grows without bound during peak, even briefly, indicates the consumer side cannot keep pace with production traffic — this is measurable as a depth-over-time curve, not a single snapshot. |
| Cache hit rate — movie catalog / detail reads | ≥ 95% at T3–T4 | Catalog content is shared across all users and changes rarely (§4.2's corpus growth is measured in titles/day, not per-request) — this is the most cacheable read path in the system, and a low hit rate here indicates a design defect, not a capacity shortfall. |
| Cache hit rate — forum/feed reads | ≥ 85% at T3–T4 | Lower target than catalog because feed content is more personalized (followed-forum composition varies per user) and changes far more frequently (§4.3's 500:1 read:write ratio still implies meaningful invalidation churn at T4 volumes). |

### 5.1 Availability Targets & Error Budget

Availability targets **tighten** as tier increases — the opposite of every other axis in this document, and deliberately so: at T4 scale, a given percentage of downtime affects orders of magnitude more real users, and user expectations of a mature platform are objectively higher than for an early product.

| | T1 | T2 | T3 | T4 |
|---|---|---|---|---|
| Availability target | 99.5% | 99.9% | 99.95% | 99.99% |
| Allowed downtime / year | 43.8 hours | 8.76 hours | 4.38 hours | 52.6 minutes |
| Allowed downtime / 30 days | 3.6 hours | 43.2 minutes | 21.6 minutes | 4.3 minutes |
| Error budget, expressed as failed requests/year (at that tier's sustained average RPS from the table above) | ~525,000 | ~2.6 million | ~2.8 billion | **~6.6 billion** |

**Why the error-budget row matters more than the percentage row:** "99.99% available" sounds uniformly excellent regardless of scale, but the *absolute number of failed user actions* it permits grows enormously with traffic — at T4, a 99.99% target still permits roughly 18,000 failed requests *per day* before the budget is exhausted. Capacity planning must be done against the request-count figure, not the percentage, because the percentage alone hides how much real user impact is actually tolerated.

**Composite/dependency-chain constraint:** any single request that traverses multiple services in serial (e.g., an authenticated write that also triggers a moderation check) has a composite availability equal to the *product* of each dependency's individual availability, which is always lower than any single dependency's own number. A target of 99.95% for the platform as a whole therefore requires every critical-path dependency to individually exceed that number, with margin — this is a structural constraint on the target, not a solution to it.

---

## 6. Database Targets

Cinemate's data is split across two datastores with fundamentally different roles: a relational store handling accounts, the movie catalog, and the social graph (durable, structured, moderate write volume), and a document store handling forum content — posts, comments, votes (very high write volume, less structural rigidity). Targets are stated per store because their workload shapes are not interchangeable.

### 6.1 Query latency

| Query class | P50 | P95 | P99 | Applies to |
|---|---|---|---|---|
| Point lookup (single-row/document by primary key) | 3 ms | 10 ms | 25 ms | Both stores |
| Simple filtered read (indexed equality/range predicate, paginated) | 8 ms | 25 ms | 60 ms | Both stores |
| Aggregation / multi-collection or multi-table join | 30 ms | 100 ms | 300 ms | Both stores; admin/analytics queries (§4.9) may use the wider dashboard-specific budget instead |
| **Maximum acceptable query time, any query, any tier** | — | — | **1 s (hard ceiling)** | A query exceeding this indicates either a missing index or an unbounded scan and must be treated as a defect regardless of which endpoint triggered it |

### 6.2 Throughput & read/write ratio

| | T1 | T2 | T3 | T4 | Unit |
|---|---|---|---|---|---|
| Relational store — read QPS (peak) | 0.7 | 16 | 290 | 3,300 | catalog reads |
| Relational store — read QPS, incl. profile + auth lookups (peak) | 1.1 | 24 | 460 | 5,480 | total |
| Relational store — write TPS (peak) | 0.03 | 0.6 | 12 | 155 | transactions/sec |
| Relational store — read : write ratio | — | ≈ 35 : 1 | | | blended across all relational-store workloads |
| Document store — read QPS (peak, forum/feed reads) | 1.0 | 24 | 440 | 5,500 | includes bundled comment/vote-status reads |
| Document store — write TPS (peak, posts + comments + votes) | 0.15 | 3.5 | 68 | 778 | writes/sec |
| Document store — read : write ratio | — | ≈ 7 : 1 | | | *(Note: this is a blended, collection-level figure and is lower than the 500:1 ratio quoted in §4.3 — that figure compares post-reads to post-writes specifically; this one includes the much more frequent vote and comment writes in the denominator.)* |

### 6.3 Index efficiency

| Metric | Target | Measurement |
|---|---|---|
| Queries served via index scan (not full collection/table scan) | ≥ 99.9% of executions | Sampled query-plan (`EXPLAIN` / equivalent) capture across production traffic, not just pre-deployment test queries |
| Index size relative to collection/table size | Tracked, not capped — flag any single index exceeding 50% of its collection's data size as requiring review | Prevents unbounded index sprawl from silently becoming its own capacity problem |

### 6.4 Connection pool sizing

Rather than specifying a fixed pool size (which is a moving target as the system's own query latency changes), the target is stated as an **outcome**, derived via Little's Law (`connections required ≈ throughput × average time each connection is held`):

| Metric | Target |
|---|---|
| Connection checkout wait time | P99 ≤ 5 ms |
| Connection checkout timeout / failure rate under peak load | 0% |
| Illustrative sizing at T4 (relational store, ~5,635 QPS peak combined read+write, ~15 ms average query hold time) | ≈ 85 concurrent connections required, **derived from measured values, not assumed** |

The illustrative T4 number above will change as the system's own query latency changes — the target is the *outcome* (zero checkout timeouts, sub-5ms wait), re-derived from live measurement at each tier, not the specific connection count.

### 6.5 Database growth

| | T1 | T2 | T3 | T4 |
|---|---|---|---|---|
| Document store — new documents/day (posts + comments + votes combined) | ~2,200 | ~50,000 | ~980,000 | **~11,200,000** |
| Document store — cumulative documents after 3 years at steady growth | ~2.4M | ~55M | ~1.1B | **~12.3B** |
| Relational store — new rows/day (accounts, reviews, social-graph edges, watch-party records, catalog submissions) | tens | hundreds | low thousands | tens of thousands |

**Why the 3-year cumulative document-store figure is the real target:** ~12 billion documents in the two highest-volume collections (comments, votes) at T4 is the number that determines whether query latency targets (§6.1) can still be met as the collections age — a system that meets every latency target on a freshly-seeded database has not demonstrated anything about whether it meets them against a multi-billion-document collection three years into production.

---

## 7. Performance Targets

This section consolidates cross-cutting performance targets that apply *across* features, complementing the per-feature latency tables in §4.

### 7.1 Consolidated API latency reference

| Feature | P50 | P95 | P99 |
|---|---|---|---|
| Token validation (§4.1) | 5 ms | 15 ms | 40 ms |
| Vote (§4.5) | 50 ms | 120 ms | 250 ms |
| Movie detail view (§4.2) | 60 ms | 180 ms | 400 ms |
| Follow/unfollow, watch-history/liked-movie writes (§4.8) | 80 ms | 200 ms | 450 ms |
| Movie catalog browse/search (§4.2) | 80 ms | 250 ms | 600 ms |
| Comment creation ack (§4.4) | 100 ms | 300 ms | 600 ms |
| Feed/forum page read (§4.3) | 100 ms | 300 ms | 700 ms |
| Watch-party control propagation, steady-state (§4.7) | 100 ms | 300 ms | 600 ms |
| Profile page load (§4.8) | 100 ms | 300 ms | 650 ms |
| Login (§4.1) | 150 ms | 400 ms | 800 ms |
| Post creation ack (§4.3) | 150 ms | 400 ms | 900 ms |
| Chat message delivery (§4.7) | 150 ms | 400 ms | 900 ms |
| Movie review submission (§4.8) | 150 ms | 400 ms | 900 ms |
| Time-to-moderation-verdict (§4.6) | 800 ms | 2 s | 5 s |
| Party join (full state sync) (§4.7) | 500 ms | 1.5 s | 3 s |
| Watch-party control propagation, flagship event (§4.7) | 250 ms | 700 ms | **1.5 s** *(explicitly relaxed tail — see §4.7)* |

### 7.2 Startup / cold-start targets

| Metric | Target | Why |
|---|---|---|
| Stateless API instance: process start → passing health check → receiving production traffic | ≤ 30 s | This bounds how quickly the fleet can react to an autoscaling event or a failed-instance replacement; it is the same target at every tier, but it matters *more* at T3–T4 where autoscaling reacts to much larger absolute traffic swings. |
| Moderation/ML inference service: process start → model loaded → first successful inference | ≤ 60 s | Model loading is an inherently heavier cold-start cost than a typical stateless service; the target acknowledges that difference explicitly rather than pretending it doesn't exist. |
| Full-stack cold environment bring-up (all dependent services healthy from zero) | ≤ 5 minutes | Relevant to disaster-recovery and environment-provisioning scenarios, not routine autoscaling. |

### 7.3 Background job / queue processing

| Metric | Target | Why |
|---|---|---|
| Queue wait time (item enqueued → processing begins), any async workload class | P95 ≤ 500 ms, P99 ≤ 2 s | Distinct from the *processing time itself* (e.g., §4.6's moderation verdict time) — this specifically measures whether the consumer side is keeping pace with production, independent of how long each item takes once picked up. |
| Scheduled/batch job completion, relative to its own defined window | Must complete before the next scheduled invocation, with ≥ 50% margin | A recurring job that takes longer than the gap between its own invocations is not a slow job — it is a queue that never drains, and the margin requirement catches this before it becomes an outage. |

### 7.4 Memory & CPU footprint under sustained load

Restated from §5 at the per-instance level for clarity: **≤ 80% memory utilization** and **≤ 70% CPU utilization** per compute instance at Tier-appropriate sustained peak load, with **zero OOM-kills over a 7-day soak test**. These are the same targets regardless of tier — what changes across tiers is the absolute load a single instance (and the fleet as a whole) must sustain while holding this ceiling.

---

## 8. Scalability Tiers

Each tier below is a complete, internally-consistent snapshot of the system's required population, throughput, and hardness profile — not merely "the same system with bigger numbers." Every tier is achievable with sound engineering; each is deliberately harder than the last in a *specific, named* way, so that progress toward this document is measured in cleared tiers, not vague improvement.

### Tier 1 — Small Deployment

| | Value |
|---|---|
| Registered users | 10,000 |
| DAU | 800 |
| Peak concurrent users | 120 |
| Peak API throughput | ~3 RPS |
| Concurrent watch parties | 2 (avg 8 users) |
| Flagship-event party size | 25 users |
| Availability target | 99.5% |

**Profile:** a real, working product with real users — a film-club community, a niche fan platform, or an early beta. Absolute load is modest; the engineering bar at this tier is **correctness under concurrency and basic resource discipline**, not raw scale. A system that cannot meet this tier's targets has a correctness or fundamentals problem, not a scale problem.

**What makes this tier non-trivial anyway:** the concurrency-correctness targets (§4.5's zero-tolerance vote-count correctness, §4.4's counter-convergence bound) apply in full even at 120 concurrent users — "small scale" is not an excuse for a lost update, and this tier is specifically designed so that passing it proves correctness, independent of capacity.

### Tier 2 — Growing Product

| | Value |
|---|---|
| Registered users | 250,000 |
| DAU | 18,000 |
| Peak concurrent users | 2,700 |
| Peak API throughput | ~60 RPS |
| Concurrent watch parties | 68 (avg 8 users) |
| Flagship-event party size | 300 users |
| Availability target | 99.9% |

**Profile:** a product with genuine traction — the scale of a funded early-stage startup with real marketing/word-of-mouth growth. This is the first tier where naive "it works on my machine" capacity (a single database instance, unindexed queries that were fast enough on a toy dataset) starts to visibly strain, and where the first real capacity-planning decisions (not yet requiring exotic architecture) become necessary.

**What makes this tier hard:** the jump from 2 to 68 concurrent watch parties, and from a 25-user to a 300-user flagship event, is the first point where WebSocket fan-out (§4.7) stops being a rounding error and starts being a real load-testing target requiring its own purpose-built harness (§9.3) rather than ad-hoc manual testing.

### Tier 3 — Large-Scale Service

| | Value |
|---|---|
| Registered users | 5,000,000 |
| DAU | 350,000 |
| Peak concurrent users | 52,500 |
| Peak API throughput | ~1,100 RPS |
| Concurrent watch parties | 1,640 (avg 8 users) |
| Flagship-event party size | 5,000 users |
| Availability target | 99.95% |
| Document-store cumulative documents (3-year) | ~1.1 billion |

**Profile:** a recognizable, established platform — the scale of a mid-size, mature social or streaming-adjacent product. At this tier, every "it was fine at Tier 2" assumption must be re-validated from scratch: 1,100 RPS sustained peak, a database approaching a billion documents, and a 5,000-user single-room synchronization target are each independently serious engineering problems.

**What makes this tier hard:** this is the first tier where the database-growth target (§6.5) — not just request throughput — becomes a first-class constraint. A system whose query latency (§6.1) was acceptable against a freshly-seeded database can fail this tier purely from data volume, even with unlimited compute capacity, which is precisely why §9.4 requires benchmarking against a realistically-aged dataset, not an empty one.

### Tier 4 — Internet-Scale Deployment

| | Value |
|---|---|
| Registered users | 50,000,000 |
| DAU | 4,000,000 |
| Peak concurrent users | 400,000 |
| Peak API throughput | ~12,600 RPS |
| Concurrent watch parties | 15,000 (avg 8 users) |
| **Flagship-event party size** | **50,000 users, single room** |
| Availability target | 99.99% (≈ 52.6 minutes/year, ≈ 6.6 billion-request error budget/year) |
| Document-store cumulative documents (3-year) | ~12.3 billion |

**Profile:** the scale of a globally-recognized platform with a genuinely global, multi-timezone user base. This tier is not "Tier 3 with a bigger budget" — it is qualitatively different in three specific ways: (1) the 50,000-person single-room synchronization stretch target is an extreme real-time systems problem independent of everything else in this document; (2) 12,600 RPS sustained peak against a 12-billion-document data store is a scale most engineers will never personally operate at; (3) a global user base introduces network-physics constraints (cross-continent RTT, typically 150–250 ms one-way) that no amount of application-layer engineering can eliminate from a single-region deployment — this is stated here as an **honest constraint on what is achievable**, not folded silently into a relaxed target, and not a suggestion of any particular remedy.

**What makes this tier hard:** everything, simultaneously, under the tightest availability budget in the document (99.99%) and with the "steady-state peak" targets barely relaxed at all relative to Tier 3 on a per-request basis — the difficulty is sustaining Tier-1-grade latency guarantees at a scale where nearly every naive implementation choice that worked at Tier 3 will not survive contact with Tier 4 traffic.

---

## 9. Benchmarking Methodology

This section defines *how* every target in §4–§7 must be measured, at a level general enough to apply across all of them. Per-metric measurement notes are given inline throughout this document; this section defines the cross-cutting rules that make those individual measurements trustworthy and comparable.

### 9.1 Test types and when each applies

| Test type | Duration | Purpose | Applies to |
|---|---|---|---|
| **Load test** (closed-loop, fixed virtual-user count) | 15–30 min per data point | Establishes the latency-vs-concurrency curve for a single endpoint/operation | Every per-feature P50/P95/P99 target in §4 |
| **Load test** (open-loop, fixed arrival rate) | 15–30 min per data point | Measures behavior under a target *throughput* rather than a target concurrency — the more realistic model for public internet traffic, where request arrival does not wait for the previous request to complete | Every RPS/QPS target in §4–§6; open-loop is the required model wherever "peak RPS" is the metric under test, since closed-loop models systematically understate real-world queueing behavior |
| **Soak test** | ≥ 7 days continuous | Detects slow leaks, unbounded growth, and gradual degradation that short tests cannot reveal | §5/§7's OOM and utilization-ceiling targets; database growth targets (§6.5) |
| **Spike/stress test** | Minutes, repeated | Validates behavior at 2–5× the tier's stated peak, beyond the modeled workload, specifically to observe *how* the system fails (graceful degradation vs. cascading failure) | Availability/error-budget targets (§5.1); this is not a target the system must "pass" in the sense of maintaining full latency SLOs — it is a test of failure *mode*, and a system that fails ungracefully at 2× peak has failed this test even if it never faces that load in production |
| **Chaos / failure-injection test** | Ongoing, scheduled | Deliberately removes or degrades a dependency (a database node, the moderation service, a cache layer, a network path) while production-shaped load continues, to validate that the composite-availability constraint in §5.1 actually holds | Every availability target; every "fail-open" or graceful-degradation expectation implied by the moderation (§4.6) and watch-party (§4.7) sections |
| **Concurrency/correctness test** (not a load test at all — a race-condition test) | Short, high-contention, repeated many times | Deliberately maximizes simultaneous writers to a single hot record (§4.5's "concurrent votes on one hot post," §4.4's counter convergence) to prove correctness under overlap, independent of throughput | §4.4, §4.5, §4.8's zero-tolerance correctness bars |

### 9.2 Workload realism

- **Traffic shape must match §2's assumptions**, not a uniform or random distribution: query/filter values should follow a Zipfian (power-law) distribution (a small number of genres/forums/movies receive most traffic), matching real content-popularity curves — uniform random load systematically understates cache effectiveness and understates hot-key contention, producing falsely optimistic results on exactly the metrics (§4.5, §5's cache-hit-rate targets) that matter most.
- **Peak, steady-state, and flagship-event scenarios must be run and reported as three separate, explicitly labeled test executions** (§2.5, §4.7) — averaging or interpolating between them is not a substitute for testing each directly, since the flagship-event scenario in particular exercises a qualitatively different code path (large-room fan-out) that may never be exercised at all during a steady-state test.
- **A tier is only "achieved" when all of that tier's targets are met simultaneously, in a single sustained test run exercising the full workload mix** (all nine feature areas concurrently, at their respective peak ratios) — not when each metric has separately passed an isolated single-feature test. Resource contention between features (e.g., the moderation service and the watch-party fan-out both competing for network bandwidth during a simultaneous peak) is itself part of what must be validated, and isolated per-feature testing cannot reveal it.

### 9.3 Watch-party fan-out testing (feature-specific methodology, referenced from §4.7)

Standard HTTP load-testing tools measure request/response latency and cannot directly observe delivery-time variance across many simultaneous WebSocket recipients. This requires a purpose-built harness that: (1) establishes N simulated persistent WebSocket connections into a single logical party/room; (2) has one designated "host" connection emit a single control event at a precisely recorded timestamp; (3) records the receipt timestamp at every one of the N connections; (4) computes the full delivery-time distribution (not just an average) across all N receipts for that single event; (5) repeats across many trials to build the P50/P95/P99 delivery-latency distribution required by §4.7.4. The steady-state scenario (N ≈ 8–20) and the flagship-event scenario (N up to the tier's stretch target) must be run as fully separate trials with separately reported distributions, per §9.2.

### 9.4 Database benchmarking

- **Never benchmark against an empty or freshly-seeded database.** Every query-latency target in §6.1 must be measured against a dataset pre-populated to the tier's stated document/row-count assumptions (§6.5) — a query that is fast against 10,000 documents and has never been measured against 1 billion has not been benchmarked against this specification's Tier 3 target, regardless of how it performs.
- **Percentile capture for database queries follows the same rolling-window methodology as §3.2**, applied at the query layer rather than the API layer, so that database-specific latency (§6.1) can be isolated from application-layer overhead when diagnosing which layer is responsible for an API-level latency miss.
- **Index-efficiency sampling (§6.3)** must be continuous in any environment used for benchmarking, not a one-time check before a test run — query plans can change as data volume and distribution shift during a long soak test, and a plan that used an index at the start of a 7-day test is not guaranteed to still be doing so at the end.

### 9.5 Reporting standard

Every benchmark run against this document should report, at minimum: the tier and scenario under test (steady-state / peak / flagship-event / soak / spike / chaos), the full P50/P95/P99 distribution (not an average) for every applicable metric, the resource-utilization curve for the duration of the test (not a single endpoint value), and an explicit pass/fail against each numbered target in §4–§7. A report that states "average latency was acceptable" without percentiles, or "throughput target met" without the corresponding resource-utilization and error-rate data, does not constitute evidence against this specification.

---

## 10. Closing Notes

This document defines the target state. It intentionally does not diagnose the current implementation, prescribe an architecture, or suggest an order of operations — that is a separate, and more interesting, conversation.

When you bring a design or an implementation approach back for review, the useful question to ask is not "does this meet the targets" in the abstract, but "at which tier, under which of §9's test types, does this specific approach start to miss a specific numbered target in this document, and why." That framing is what turns this specification from a checklist into the benchmark it's meant to be.