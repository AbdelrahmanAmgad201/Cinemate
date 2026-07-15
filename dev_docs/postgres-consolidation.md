# PostgreSQL Consolidation — Phase 2 Schema Design

> **Status:** ✅ Implemented — this is the current data layer (schema lives in
> `backend/src/main/resources/db/migration/V1__baseline.sql`, owned by Flyway). The
> document is retained as the authoritative design rationale for how and why the store
> was consolidated. Supersedes the old MySQL + MongoDB + response-cache design.
> Locked scope (from Phase 1):
> - MySQL **and** MongoDB → one PostgreSQL database.
> - Response cache (`exploreFeed`/`exploreForum`) **removed**.
> - Refresh tokens **move to Postgres**; gateway rate-limiting **stays on its own Redis**;
>   watch-party's durable Redis **out of scope**.
> - **Greenfield schema** — no data migration, dev data regenerated.
> - Kafka moderation pipeline **preserved**; the outbox becomes a Postgres table.

---

## 0. Two decisions carried over from Phase 1 (with recommendations)

### D1 — ID strategy: **hybrid** (recommended)

| Domain | ID type | Why |
|---|---|---|
| Identity/catalog core: `users`, `admins`, `organizations`, `movies`, and their join tables | **`BIGINT` identity** | Already `BIGINT`; compact (8B); low volume; changing them would ripple into the gateway/JWT `X-User-Id` contract for **zero** benefit. Keeping them unchanged makes the whole migration lower-risk. |
| Social content: `forums`, `posts`, `comments`, `post_votes`, `comment_votes`, `forum_follows` | **`UUID` (v7)** | (a) Replaces `ObjectId`'s time-ordered property natively (v7 is timestamp-prefixed → index-friendly, no B-tree hot-spotting); (b) **non-enumerable** — you can't scrape `/posts/1,2,3…`; (c) generatable app-side without a DB round-trip; (d) shardable later with no central sequence — directly serves the "designed for scale" goal and the deferred wide-column option. |

**Trade-off:** two ID types is marginally more cognitive load than "one type everywhere."
The all-`BIGINT` alternative is simpler but makes every social row enumerable and re-introduces
a central sequence as a future sharding obstacle. The hybrid draws the line exactly where the
workload changes character. UUIDv7 needs Postgres 18 (native `uuidv7()`) or a small app-side
generator on older versions — noted for Phase 3.

### D2 — Delete strategy: **soft-delete only where it's load-bearing** (recommended)

Today *everything* soft-deletes (a Mongo-driven uniformity). Challenged and split by actual need:

| Data | Strategy | Reason |
|---|---|---|
| `forums`, `posts`, `comments` | **Soft delete** (`is_deleted`, `deleted_at`) + scheduled hard-purge | These are **moderatable and restorable** (MOD-08: admins can restore; the verdict consumer does version-guarded removal). Soft-delete is genuinely load-bearing here. |
| `post_votes`, `comment_votes` | **Hard delete / upsert** | A vote is never moderated or restored. "Change vote" = flip `vote_type`; "remove vote" = delete row. The soft-delete + "reactivate in place" dance only existed to satisfy a Mongo unique index — a real `UNIQUE` + upsert is cleaner. |
| `forum_follows`, `liked_movies`, `watch_later`, `user_follows` | **Hard delete** | Pure toggles. "Unfollow" means the relationship is gone. `UNIQUE` constraint makes re-adding a plain insert. Removes all reactivation logic. |
| `watch_history` | **Hard delete** | It's the user's own append-only log; removing an item is a row delete. |

**Cascade mechanics:** with soft-delete, deleting a post/forum can't use FK `ON DELETE CASCADE`
(the row stays). So:
- **Soft-delete cascade** = one recursive-CTE `UPDATE` that flips `is_deleted` on the subtree
  (replaces `CascadeDeletionService`'s `$graphLookup` + async batching, ~450 lines).
- **Physical cleanup** = the scheduled purge does a real `DELETE`, and FK `ON DELETE CASCADE`
  removes the children (comments, votes) automatically at that point.

**Trade-off:** two deletion modes to reason about. But each is now a handful of lines, versus
the current large async cascade service.

---

## 1. Cross-cutting conventions

- **Timestamps:** everything is `TIMESTAMPTZ` (maps to `Instant`). Drops the current mix of
  `LocalDateTime` (MySQL, zone-less, bug-prone) and `Instant` (Mongo). Genuine correctness fix.
- **Enums** (`Genre`, `State`, `Gender`, `WatchPartyStatus`, `ModerationStatus`, `VoteTargetType`):
  `VARCHAR` + `CHECK (col IN (...))`. Matches today's `@Enumerated(STRING)` storage, and unlike a
  native PG `ENUM` it evolves without `ALTER TYPE`. `moderation_status` defaults to `'PENDING'`.
- **Soft-delete mixin:** a JPA `@MappedSuperclass` (`SoftDeletable`) supplying
  `is_deleted BOOLEAN NOT NULL DEFAULT false` + `deleted_at TIMESTAMPTZ NULL`, applied only to the
  three tables that keep soft-delete.
- **Derived counters** — three mechanisms, chosen per shape (kills the "hand-maintained counter
  drift" bug class from Phase 1 §4):
  - `score` → **`GENERATED ALWAYS AS (upvote_count - downvote_count) STORED`** (posts, comments).
  - Parent/child aggregates with a **non-polymorphic** source → **`AFTER` trigger**
    (`posts.comment_count`, `forums.post_count`, `forums.follower_count`,
    `users.number_of_followers`/`number_of_following`, `post_votes`/`comment_votes` → their
    target's `upvote_count`/`downvote_count`). Splitting votes into two typed tables (see §4.5) is
    what lets these triggers stay simple.
  - `movies.average_rating` → **`GENERATED ALWAYS AS (CASE WHEN rating_count = 0 THEN 0 ELSE
    rating_sum::numeric / rating_count END) STORED`** (removes manual `averageRating` upkeep).

  **Trade-off:** triggers are "invisible" logic. Accepted deliberately: they make the counters
  *impossible* to drift (atomic with the row change), which the current app-maintained counters
  are not. Documented in one place.
- **Schema delivery:** introduce **Flyway** (`V1__baseline.sql`, …). Today `ddl-auto=validate`
  with no migration tool means the schema is created implicitly — a Postgres-first design should
  be migration-driven. `ddl-auto` stays `validate` (Hibernate checks the entities against Flyway's
  schema; Flyway owns DDL).

---

## 2. Identity & auth

```sql
-- three-role auth kept as-is (the User/Admin/Organization split is a separate concern,
-- Lesson 5 — out of scope here). Only the store changes.
users(
  user_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  first_name         VARCHAR(255),
  last_name          VARCHAR(255),
  email              VARCHAR(255) NOT NULL UNIQUE,
  password           VARCHAR(255),               -- null for OAuth users
  gender             VARCHAR(16)  CHECK (gender IN ('MALE','FEMALE','OTHER')),
  birth_date         DATE,
  about              TEXT,
  is_public          BOOLEAN NOT NULL DEFAULT true,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  provider           VARCHAR(32)  NOT NULL DEFAULT 'local',
  provider_id        VARCHAR(255),
  profile_complete   BOOLEAN NOT NULL DEFAULT true,
  number_of_followers INTEGER NOT NULL DEFAULT 0, -- trigger-maintained
  number_of_following INTEGER NOT NULL DEFAULT 0  -- trigger-maintained
);

admins(admin_id BIGINT … PK, name VARCHAR(255), email … NOT NULL UNIQUE, password NOT NULL);
organizations(organization_id BIGINT … PK, name, email … NOT NULL UNIQUE, password NOT NULL,
              about TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now());

verifications(email VARCHAR(255) PRIMARY KEY, password VARCHAR(255), code VARCHAR(60),
              role VARCHAR(32) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now());

-- NEW: replaces the Redis refresh-token store (RefreshTokenService).
refresh_tokens(
  token_hash  CHAR(64) PRIMARY KEY,          -- SHA-256 hex of the opaque token (unchanged hashing)
  role        VARCHAR(32)  NOT NULL,
  email       VARCHAR(255) NOT NULL,
  expires_at  TIMESTAMPTZ  NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);   -- cleanup sweep
```

**Refresh-token semantics preserved:** single-use rotation was Redis `GETDEL`; the Postgres
equivalent is `DELETE FROM refresh_tokens WHERE token_hash = ? RETURNING role, email` — atomic
read-and-invalidate, same guarantee. TTL (Redis auto-expiry) becomes a scheduled
`DELETE … WHERE expires_at < now()` (reuses the existing cleanup-scheduler pattern) plus an
`expires_at > now()` filter on lookup so an un-purged expired token is never accepted.

**Indexes:** `users`/`admins`/`organizations` — `UNIQUE(email)` already covers login lookups
(the separate `idx_*_email` become redundant and are dropped).

## 3. Movie catalog & user–movie interactions

```sql
movies(
  movie_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name            VARCHAR(255) NOT NULL,
  description     TEXT,
  movie_url       VARCHAR(1024),
  thumbnail_url   VARCHAR(1024),
  trailer_url     VARCHAR(1024),
  duration        INTEGER,
  genre           VARCHAR(20) CHECK (genre IN (...)),      -- Genre enum
  release_date    DATE,
  rating_sum      BIGINT  NOT NULL DEFAULT 0,
  rating_count    INTEGER NOT NULL DEFAULT 0,
  average_rating  NUMERIC(4,2) GENERATED ALWAYS AS
                    (CASE WHEN rating_count = 0 THEN 0 ELSE rating_sum::numeric/rating_count END) STORED,
  organization_id BIGINT NOT NULL REFERENCES organizations(organization_id),
  admin_id        BIGINT REFERENCES admins(admin_id)       -- nullable (unapproved)
);
CREATE INDEX idx_movie_genre  ON movies(genre);
CREATE INDEX idx_movie_rating ON movies(average_rating);
CREATE INDEX idx_movie_admin  ON movies(admin_id);
-- FK organization_id also gets an index (join from org → movies).

movie_reviews(
  movie_id   BIGINT NOT NULL REFERENCES movies(movie_id)  ON DELETE CASCADE,
  reviewer_id BIGINT NOT NULL REFERENCES users(user_id)   ON DELETE CASCADE,
  rating     SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 10),   -- was a @PrePersist check
  comment    TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (movie_id, reviewer_id)
);
CREATE INDEX idx_review_reviewer ON movie_reviews(reviewer_id);
-- rating_sum/rating_count on movies maintained by an AFTER trigger on movie_reviews.

liked_movies(
  user_id  BIGINT NOT NULL REFERENCES users(user_id)  ON DELETE CASCADE,
  movie_id BIGINT NOT NULL REFERENCES movies(movie_id) ON DELETE CASCADE,
  liked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, movie_id)          -- movie_name DROPPED → join to movies
);
CREATE INDEX idx_liked_user ON liked_movies(user_id);

watch_later(   -- same shape as liked_movies; movie_name DROPPED
  user_id BIGINT … REFERENCES users ON DELETE CASCADE,
  movie_id BIGINT … REFERENCES movies ON DELETE CASCADE,
  added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, movie_id));

watch_history(  -- append-only log; movie_id becomes a REAL FK (was a raw Long), movie_name DROPPED
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id    BIGINT NOT NULL REFERENCES users(user_id)  ON DELETE CASCADE,
  movie_id   BIGINT NOT NULL REFERENCES movies(movie_id) ON DELETE CASCADE,
  watched_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE INDEX idx_wh_user ON watch_history(user_id, watched_at DESC);

user_follows(   -- user→user; isDeleted DROPPED (hard delete)
  follower_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  followee_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  followed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (follower_id, followee_id),
  CHECK (follower_id <> followee_id));                 -- can't follow yourself (new constraint)
CREATE INDEX idx_follows_followee ON user_follows(followee_id);   -- "who follows X"
-- users.number_of_followers/following maintained by an AFTER trigger here.

requests(   -- org-publish workflow, unchanged shape
  id BIGINT … PK, movie_name VARCHAR(255), created_at TIMESTAMPTZ, state_update_at TIMESTAMPTZ,
  state VARCHAR(32) CHECK (...), admin_id BIGINT REFERENCES admins,
  organization_id BIGINT NOT NULL REFERENCES organizations,
  movie_id BIGINT REFERENCES movies);

watch_parties(   -- stays relational (was already MySQL). Redis live-state untouched.
  id BIGINT … PK, party_id VARCHAR(255) NOT NULL UNIQUE,
  movie_id BIGINT NOT NULL REFERENCES movies, user_id BIGINT NOT NULL REFERENCES users,
  status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','ENDED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), ended_at TIMESTAMPTZ);
CREATE INDEX idx_watch_party_party_id ON watch_parties(party_id);   -- (redundant w/ UNIQUE; keep only if lookups need it)
```

## 4. Social content (was MongoDB)

### 4.1 forums
```sql
forums(
  id             UUID PRIMARY KEY DEFAULT uuidv7(),
  owner_id       BIGINT NOT NULL REFERENCES users(user_id),   -- was fabricated ObjectId
  name           VARCHAR(255) NOT NULL,
  description    TEXT NOT NULL,
  follower_count INTEGER NOT NULL DEFAULT 0,   -- trigger-maintained (forum_follows)
  post_count     INTEGER NOT NULL DEFAULT 0,   -- trigger-maintained (posts)
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  moderation_status  VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (...),
  moderation_version BIGINT NOT NULL DEFAULT 1,
  is_deleted     BOOLEAN NOT NULL DEFAULT false,
  deleted_at     TIMESTAMPTZ,
  search_tsv     tsvector GENERATED ALWAYS AS
                   (to_tsvector('english', name || ' ' || description)) STORED
);
CREATE INDEX idx_forum_owner   ON forums(owner_id);
CREATE INDEX idx_forum_search  ON forums USING GIN (search_tsv);          -- replaces Mongo @TextIndexed
-- explore-forums (is_deleted=false, sort by follower_count/post_count/created_at):
CREATE INDEX idx_forum_explore_followers ON forums(follower_count DESC) WHERE NOT is_deleted;
CREATE INDEX idx_forum_explore_posts     ON forums(post_count     DESC) WHERE NOT is_deleted;
CREATE INDEX idx_forum_explore_new       ON forums(created_at     DESC) WHERE NOT is_deleted;
```

### 4.2 posts
```sql
posts(
  id             UUID PRIMARY KEY DEFAULT uuidv7(),
  forum_id       UUID   NOT NULL REFERENCES forums(id) ON DELETE CASCADE,
  owner_id       BIGINT NOT NULL REFERENCES users(user_id),
  title          VARCHAR(300) NOT NULL,
  content        TEXT,
  upvote_count   INTEGER NOT NULL DEFAULT 0,   -- trigger-maintained (post_votes)
  downvote_count INTEGER NOT NULL DEFAULT 0,   -- trigger-maintained
  score          INTEGER GENERATED ALWAYS AS (upvote_count - downvote_count) STORED,
  comment_count  INTEGER NOT NULL DEFAULT 0,   -- trigger-maintained (comments)
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_activity_at TIMESTAMPTZ,
  moderation_status  VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (...),
  moderation_version BIGINT NOT NULL DEFAULT 1,
  is_deleted     BOOLEAN NOT NULL DEFAULT false,
  deleted_at     TIMESTAMPTZ
  -- forum_name, author_name DROPPED → joined from forums / users
);
-- forum posts (forum_id + is_deleted, sort created_at | score | last_activity_at):
CREATE INDEX idx_post_forum_new   ON posts(forum_id, created_at       DESC) WHERE NOT is_deleted;
CREATE INDEX idx_post_forum_score ON posts(forum_id, score            DESC) WHERE NOT is_deleted;
CREATE INDEX idx_post_forum_hot   ON posts(forum_id, last_activity_at DESC) WHERE NOT is_deleted;
-- explore feed (is_deleted + created_at >= cutoff, sort score/new/hot):
CREATE INDEX idx_post_explore_new   ON posts(created_at DESC) WHERE NOT is_deleted;
CREATE INDEX idx_post_explore_score ON posts(score DESC, created_at DESC) WHERE NOT is_deleted;
-- user's posts / followed-forums feed:
CREATE INDEX idx_post_owner ON posts(owner_id, created_at DESC) WHERE NOT is_deleted;
```
Each index maps 1:1 to a current Mongo compound index, but as a **partial index** (`WHERE NOT
is_deleted`) — every Mongo index was already prefixed by `isDeleted`, so partial indexes are the
faithful *and* smaller translation.

### 4.3 comments
```sql
comments(
  id             UUID PRIMARY KEY DEFAULT uuidv7(),
  post_id        UUID   NOT NULL REFERENCES posts(id)    ON DELETE CASCADE,
  parent_id      UUID   REFERENCES comments(id)          ON DELETE CASCADE,   -- null = top-level
  owner_id       BIGINT NOT NULL REFERENCES users(user_id),
  content        TEXT NOT NULL,
  upvote_count   INTEGER NOT NULL DEFAULT 0,   -- trigger-maintained (comment_votes)
  downvote_count INTEGER NOT NULL DEFAULT 0,
  score          INTEGER GENERATED ALWAYS AS (upvote_count - downvote_count) STORED,
  depth          SMALLINT NOT NULL DEFAULT 0,
  number_of_replies INTEGER NOT NULL DEFAULT 0,   -- trigger-maintained (direct children)
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  moderation_status  VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (...),
  moderation_version BIGINT NOT NULL DEFAULT 1,
  is_deleted     BOOLEAN NOT NULL DEFAULT false,
  deleted_at     TIMESTAMPTZ
  -- forum_id, post_owner_id DROPPED → access checks join comments→posts→forums (cheap on PK/FK)
);
CREATE INDEX idx_comment_post_new   ON comments(post_id, created_at) WHERE NOT is_deleted;
CREATE INDEX idx_comment_post_score ON comments(post_id, score DESC) WHERE NOT is_deleted;
CREATE INDEX idx_comment_parent     ON comments(parent_id, created_at) WHERE NOT is_deleted;
```
**Recursive delete** (replaces `$graphLookup`): soft-delete a subtree with
```sql
WITH RECURSIVE subtree AS (
  SELECT id FROM comments WHERE id = :root
  UNION ALL
  SELECT c.id FROM comments c JOIN subtree s ON c.parent_id = s.id)
UPDATE comments SET is_deleted = true, deleted_at = now()
WHERE id IN (SELECT id FROM subtree);
```
The physical purge is a plain `DELETE FROM comments WHERE id = :root` — `ON DELETE CASCADE` on the
self-FK removes the whole subtree.

### 4.4 forum_follows (was `following`)
```sql
forum_follows(
  user_id    BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  forum_id   UUID   NOT NULL REFERENCES forums(id)     ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, forum_id)      -- surrogate ObjectId id DROPPED; natural key + hard delete
);
CREATE INDEX idx_forum_follows_user  ON forum_follows(user_id, created_at DESC);  -- "forums I follow"
CREATE INDEX idx_forum_follows_forum ON forum_follows(forum_id);                  -- "followers of forum"
-- forums.follower_count maintained by an AFTER trigger here.
```

### 4.5 Votes → **two typed tables** (was one polymorphic `votes` collection)
```sql
post_votes(
  user_id    BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  post_id    UUID   NOT NULL REFERENCES posts(id)      ON DELETE CASCADE,
  vote_type  SMALLINT NOT NULL CHECK (vote_type IN (-1, 1)),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, post_id));
CREATE INDEX idx_post_votes_post ON post_votes(post_id);

comment_votes(  -- identical shape, → comments(id)
  user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  comment_id UUID NOT NULL REFERENCES comments(id)  ON DELETE CASCADE,
  vote_type SMALLINT NOT NULL CHECK (vote_type IN (-1,1)),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, comment_id));
CREATE INDEX idx_comment_votes_comment ON comment_votes(comment_id);
```
**Why split** — this is the sharpest "challenge the old design" call. The polymorphic
`(targetType, targetId)` collection was a document-store idiom that made a real FK impossible
(the integrity gap from Phase 1). Two typed tables give: real FK integrity, `ON DELETE CASCADE`
(deleting a post deletes its votes — no cascade code), a natural PK that enforces one-vote-per-user
(was a Mongo unique index), and **simple non-polymorphic count triggers** (each table maintains its
own target's `upvote_count`/`downvote_count`). "Change my vote" = upsert on the PK;
"remove my vote" = delete.
**Trade-off:** two tables and two (symmetric) service paths instead of one — mediated by a shared
generic vote handler so business logic isn't duplicated.

## 5. Moderation outbox (was a Mongo collection)
```sql
moderation_outbox(
  id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- monotonic → preserves per-content order
  content_type    VARCHAR(16) NOT NULL,    -- POST | COMMENT | FORUM
  content_id      UUID NOT NULL,
  content_version BIGINT NOT NULL,
  text            TEXT NOT NULL,           -- snapshot moderated
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now());
```
This is the **biggest architectural simplification**. The outbox's whole reason for the Mongo
transaction machinery (`TransactionConfig`'s second manager, `mongoTransactionOperations`, the
single-node replica-set requirement MOD-07) was to commit *content + outbox entry atomically*.
In one Postgres database that's just an ordinary `@Transactional` method — the insert of the post
and the insert of the outbox row are in the same transaction by default. **All of that machinery
is deleted.** The `OutboxRelay` poller changes only its query (`SELECT … ORDER BY id LIMIT :batch`);
as a bonus, the MOD-03 multi-instance duplicate-publish issue becomes fixable for free with
`FOR UPDATE SKIP LOCKED` (not required now, noted).

---

## 6. What changes in entity ownership & relationships (summary)

| # | Change | Kind |
|---|---|---|
| 1 | `owner_id`/`forum_id`/`post_id`/`parent_id`/`user_id` on social content become **real FKs** | Integrity gain |
| 2 | `IdConverter` (Long↔ObjectId bridge) **deleted** | Complexity removed |
| 3 | `Post.forum_name`/`author_name`, `*.movie_name` denormalization **removed** → joins | Staleness removed |
| 4 | `WatchHistory.movieId` raw Long → **FK to movies** | Integrity gain |
| 5 | `Comment.forum_id`, `post_owner_id` denormalization **removed** → joins | Simplification |
| 6 | Polymorphic `votes` → **`post_votes` + `comment_votes`** | Integrity + simpler triggers |
| 7 | `following` surrogate id → **composite natural key** `forum_follows` | Simplification |
| 8 | Refresh tokens Redis → **`refresh_tokens` table** | Store change (locked scope) |
| 9 | Outbox Mongo collection → **`moderation_outbox` table**, shares content tx natively | Major simplification |
| 10 | Hand-maintained counters → **triggers + generated columns** | Correctness (no drift) |
| 11 | Second transaction manager + `mongoTransactionOperations` **deleted** | Complexity removed |
| 12 | Most of `CascadeDeletionService` (`$graphLookup`, async batch) → **recursive CTE + FK cascade** | ~450 lines removed |
| 13 | `LocalDateTime`/`Instant` mix → **`TIMESTAMPTZ` everywhere** | Correctness |
| 14 | Response cache (`@Cacheable`, `CacheConfig`) **removed** → direct indexed queries | Locked scope |

**Explicitly NOT changing** (out of scope, flagged): the three-role `User/Admin/Organization`
split (Lesson 5 — its own refactor), the watch-party microservice + its durable Redis (ARC-02),
the Kafka pipeline's shape, and the gateway rate-limiter's Redis.

---

## 7. Deferred / not-doing-now (avoiding premature optimization)

- **No materialized view for the explore feed yet.** With the partial indexes in §4.1–4.2 the
  explore queries are cheap; a MV (or `pg_cron` refresh) is the documented fallback *if* Phase 4
  `EXPLAIN ANALYZE` shows a real cost — not a day-one build. This is the honest replacement for the
  cache we're removing.
- **No post/comment full-text search yet** (only forums, which had it). The `posts.search_tsv`
  column is easy to add later; not adding it now keeps scope to "preserve existing functionality."
- **No `FOR UPDATE SKIP LOCKED` relay leader-election** (MOD-03) — noted as newly-trivial, deferred.

---

## 8. Open question for approval

The design above uses **triggers** for aggregate counters. The alternative is to keep counter
maintenance in the **service layer** (now atomic anyway, since it's one DB). Triggers = can't
drift, invisible; service-layer = visible, but must be remembered on every write path. My
recommendation is triggers for the pure parent/child aggregates (listed in §1) because correctness
should be structural, not a discipline. If you'd prefer service-layer maintenance for visibility,
that's a one-word change to this plan.

Everything here is design only. On approval I'll proceed to **Phase 3** (Flyway baseline + entity/
repository/service rewrites + config/compose/docs), incrementally and with tests.

---

## 9. Implementation status (Phase 3/4)

**Done.** MySQL + MongoDB + the response-cache Redis are gone; the backend runs on a single
PostgreSQL database (`postgres:16`) with Flyway (`V1__baseline.sql`) owning the schema. The
backend has **zero** Redis dependency; the only Redis left system-wide is the gateway's
rate-limiter (`redis-ratelimit`). `mvnw compile` is green; surviving unit tests pass.

Highlights delivered: real FKs everywhere (the `IdConverter` Long↔ObjectId bridge is deleted),
the moderation outbox is an ordinary Postgres table committed in the same transaction as the
content it describes (**ARC-01 resolved** — no cross-store boundary), votes split into
`post_votes`/`comment_votes`, `Following`→`forum_follows`, refresh tokens + OAuth exchange codes
moved to Postgres tables, cascade soft-delete via a recursive CTE, and `TransactionConfig` /
`CacheConfig` / dual transaction managers removed.

### Deviations from the design above (all "smallest correct change")
- **Relational join tables kept their `movieName` + `isDeleted`** (`liked_movies`, `watch_later`,
  `watch_history`, `follows`). Converting them added projection/service churn for no real gain.
- **Movie-rating and user-follower counters stay app-maintained** (they already were, and are
  transactional now). Triggers cover only the social counters Mongo maintained in app code.
- **Relational timestamps stay `LocalDateTime`/`TIMESTAMP`** (not `Instant`/`TIMESTAMPTZ`) to
  avoid a DTO-wide ripple and date-format changes. Social/token tables use `TIMESTAMPTZ`.
- **IDs:** BIGINT for identity/catalog; UUIDv7 (generated app-side via `UuidV7`, since PG16 lacks
  native `uuidv7()`) for social content.
- Casting a vote no longer bumps a post's `lastActivityAt` (minor "hot"-ranking change).
- Forum search stays substring `ILIKE` (as the Mongo version actually behaved); the tsvector/GIN
  column was dropped as dead weight. Scalable search (full-text / `pg_trgm`) is a follow-up.

### Review findings fixed (bugs a compiling build hid — Postgres-specific)
1. **Verdict consumer could un-delete removed content:** a managed-entity dirty-flush would
   overwrite the `is_deleted` a bulk soft-delete set. Moderation-status now goes through bulk
   `@Modifying` queries; the loaded entity is read-only.
2. **Purge crashed on content with votes/follows:** FK-cascade delete fired count triggers that
   `UPDATE` the row being deleted. Guarded those trigger DELETE branches with `AND is_deleted = false`.
3. **Bulk subtree/forum soft-delete crashed on reply counts:** `number_of_replies` was a
   `comments→comments` trigger. Moved to atomic app-side `@Modifying` increments.
4. Seeder (`DataInitializer`) violated the new FKs by referencing not-yet-created users; reordered
   to create all users first.

### Tests (Phase 4 — partial)
Per decision, the Mongo/social/vote/moderation test suite was **deleted** (they tested Mongo
internals — `MongoTemplate` mocks, `ObjectId`, `mongoTransactionOperations`) to be **rewritten
fresh** against the JPA implementation later. The MySQL-domain tests (movie, user, admin,
organization, verification, security, watch-*) survive and were repointed from the MySQL/Mongo/Redis
Testcontainer bases to a single `AbstractPostgresIntegrationTest` (Flyway-built schema).

### To verify on a machine with Docker (couldn't run here)
- `docker compose up -d --build` — Flyway applies `V1__baseline.sql`; exercise create/vote/comment/
  delete/moderate flows.
- The `@DataJpaTest`/`@SpringBootTest` suites (need a Postgres Testcontainer).
- `Instant`↔`timestamptz` under `ddl-auto=validate` — the standard mapping should pass; if not, set
  `hibernate.type.preferred_instant_jdbc_type=TIMESTAMP_WITH_TIMEZONE`.
