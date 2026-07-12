-- =============================================================================
-- Cinemate — PostgreSQL baseline schema (V1)
-- Consolidates the former MySQL + MongoDB split into one database.
-- See dev_docs/postgres-consolidation.md for the design rationale.
--
-- Conventions:
--   * Timestamps: social content + auth-token tables use TIMESTAMPTZ (mapped to
--     Instant). The identity/catalog tables carried over from MySQL keep their
--     LocalDateTime fields, so their columns are plain TIMESTAMP — converting those to
--     Instant would ripple through many DTOs and change date JSON with no correctness
--     gain here. Documented deviation; a follow-up can unify on Instant/TIMESTAMPTZ.
--   * Enums are VARCHAR + CHECK (matches JPA @Enumerated(STRING); evolves without ALTER TYPE).
--   * BIGINT identity for the identity/catalog core; UUID (v7, generated app-side) for
--     high-volume, externally-exposed social content.
--   * Derived counters: GENERATED columns for score / average_rating; AFTER triggers
--     (bottom of file) for the social counters that Mongo previously maintained in app
--     code. The already-working MySQL counters (movie rating aggregate, user follower
--     counts) stay app-maintained — they're transactional now anyway in one DB.
--   * Soft delete (is_deleted/deleted_at) on forums/posts/comments (moderatable, via the
--     SoftDeletable mixin). The MySQL join tables keep their existing is_deleted flag.
-- =============================================================================

-- ─── Identity & auth ─────────────────────────────────────────────────────────

CREATE TABLE users (
    user_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name          VARCHAR(255),
    last_name           VARCHAR(255),
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255),                 -- null for OAuth-only users
    gender              VARCHAR(16) CHECK (gender IN ('MALE','FEMALE')),
    birth_date          DATE,
    about               TEXT,
    is_public           BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    provider            VARCHAR(32) NOT NULL DEFAULT 'local',
    provider_id         VARCHAR(255),
    profile_complete    BOOLEAN NOT NULL DEFAULT true,
    number_of_followers INTEGER NOT NULL DEFAULT 0,   -- app-maintained (FollowService)
    number_of_following INTEGER NOT NULL DEFAULT 0    -- app-maintained (FollowService)
);

CREATE TABLE admins (
    admin_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name     VARCHAR(255),
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE organizations (
    organization_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(255),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    about           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE verifications (
    email      VARCHAR(255) PRIMARY KEY,
    password   VARCHAR(255),
    code       VARCHAR(60),                           -- BCrypt hash of the code (SEC-10)
    role       VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Opaque, single-use refresh tokens (was Redis; see RefreshTokenService).
-- Rotation = DELETE ... RETURNING (atomic read-and-invalidate, the Redis GETDEL analogue).
CREATE TABLE refresh_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,               -- SHA-256 hex of the raw token
    role       VARCHAR(32)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);

-- Short-lived, single-use OAuth redirect handoff codes (was Redis; see OAuthExchangeService).
CREATE TABLE oauth_exchange_codes (
    code       UUID PRIMARY KEY,
    token      TEXT        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_oauth_codes_expires ON oauth_exchange_codes (expires_at);

-- ─── Movie catalog ───────────────────────────────────────────────────────────

CREATE TABLE movies (
    movie_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    movie_url       VARCHAR(1024),
    thumbnail_url   VARCHAR(1024),
    trailer_url     VARCHAR(1024),
    duration        INTEGER,
    genre           VARCHAR(20) CHECK (genre IN
                      ('MYSTERY','COMEDY','ANIMATION','DOCUMENTARY','ROMANCE',
                       'THRILLER','SCIFI','HORROR','DRAMA','ACTION')),
    release_date    DATE,
    rating_sum      BIGINT  NOT NULL DEFAULT 0,       -- app-maintained (MovieReviewService)
    rating_count    INTEGER NOT NULL DEFAULT 0,       -- app-maintained (MovieReviewService)
    average_rating  DOUBLE PRECISION GENERATED ALWAYS AS
                      (CASE WHEN rating_count = 0 THEN 0
                            ELSE rating_sum::double precision / rating_count END) STORED,
    organization_id BIGINT NOT NULL REFERENCES organizations (organization_id),
    admin_id        BIGINT REFERENCES admins (admin_id)
);
CREATE INDEX idx_movie_genre  ON movies (genre);
CREATE INDEX idx_movie_rating ON movies (average_rating);
CREATE INDEX idx_movie_admin  ON movies (admin_id);
CREATE INDEX idx_movie_org    ON movies (organization_id);

CREATE TABLE movie_review (
    movie_id    BIGINT   NOT NULL REFERENCES movies (movie_id) ON DELETE CASCADE,
    reviewer_id BIGINT   NOT NULL REFERENCES users (user_id)   ON DELETE CASCADE,
    rating      INTEGER  NOT NULL CHECK (rating BETWEEN 1 AND 10),
    comment     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (movie_id, reviewer_id)
);
CREATE INDEX idx_review_reviewer ON movie_review (reviewer_id);

-- The MySQL join tables keep their denormalized movie_name and is_deleted soft-delete
-- flag (unchanged behavior; consolidation of those is a documented follow-up).
CREATE TABLE liked_movies (
    user_id    BIGINT NOT NULL REFERENCES users (user_id)   ON DELETE CASCADE,
    movie_id   BIGINT NOT NULL REFERENCES movies (movie_id) ON DELETE CASCADE,
    movie_name VARCHAR(255),
    date       TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (user_id, movie_id)
);
CREATE INDEX idx_liked_user ON liked_movies (user_id);

CREATE TABLE watch_later (
    user_id    BIGINT NOT NULL REFERENCES users (user_id)   ON DELETE CASCADE,
    movie_id   BIGINT NOT NULL REFERENCES movies (movie_id) ON DELETE CASCADE,
    movie_name VARCHAR(255) NOT NULL,
    date       TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (user_id, movie_id)
);
CREATE INDEX idx_watch_later_user ON watch_later (user_id);

-- movie_id is a real FK now (was a raw Long).
CREATE TABLE watch_history (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (user_id)   ON DELETE CASCADE,
    movie_id   BIGINT NOT NULL REFERENCES movies (movie_id) ON DELETE CASCADE,
    movie_name VARCHAR(255) NOT NULL,
    watched_at TIMESTAMP NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX idx_wh_user ON watch_history (user_id, watched_at DESC);

-- user → user follows. Keeps its is_deleted flag; counts stay app-maintained (FollowService).
CREATE TABLE follows (
    following_user_id BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    followed_user_id  BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    followed_at       TIMESTAMP NOT NULL DEFAULT now(),
    is_deleted        BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (following_user_id, followed_user_id)
);
CREATE INDEX idx_followed_user ON follows (followed_user_id);

CREATE TABLE requests_mails (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    movie_name      VARCHAR(255),
    created_at      TIMESTAMP,
    state_update_at TIMESTAMP,
    state           VARCHAR(16) CHECK (state IN ('ACCEPTED','REJECTED','PENDING')),
    admin_id        BIGINT REFERENCES admins (admin_id),
    organization_id BIGINT NOT NULL REFERENCES organizations (organization_id),
    movie_id        BIGINT REFERENCES movies (movie_id)
);

CREATE TABLE watch_parties (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    party_id   VARCHAR(255) NOT NULL UNIQUE,
    movie_id   BIGINT NOT NULL REFERENCES movies (movie_id),
    user_id    BIGINT NOT NULL REFERENCES users (user_id),
    status     VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','ENDED')),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    ended_at   TIMESTAMP
);
CREATE INDEX idx_watch_party_party_id ON watch_parties (party_id);

-- ─── Social content (was MongoDB) ────────────────────────────────────────────

CREATE TABLE forums (
    id                 UUID PRIMARY KEY,
    owner_id           BIGINT NOT NULL REFERENCES users (user_id),
    name               VARCHAR(255) NOT NULL,
    description        TEXT NOT NULL,
    follower_count     INTEGER NOT NULL DEFAULT 0,    -- trigger-maintained (forum_follows)
    post_count         INTEGER NOT NULL DEFAULT 0,    -- trigger-maintained (posts, is_deleted-aware)
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    moderation_status  VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                         CHECK (moderation_status IN ('PENDING','APPROVED','REMOVED')),
    moderation_version BIGINT NOT NULL DEFAULT 1,
    is_deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at         TIMESTAMPTZ
    -- Forum search is substring ILIKE (findByNameContainingIgnoreCase...), same as the
    -- Mongo version's actual behavior. A tsvector/GIN column would be pure write-overhead
    -- for a query that can't use it; scalable search (full-text or pg_trgm) is a follow-up.
);
CREATE INDEX idx_forum_owner  ON forums (owner_id);
CREATE INDEX idx_forum_explore_followers ON forums (follower_count DESC) WHERE NOT is_deleted;
CREATE INDEX idx_forum_explore_posts     ON forums (post_count     DESC) WHERE NOT is_deleted;
CREATE INDEX idx_forum_explore_new       ON forums (created_at     DESC) WHERE NOT is_deleted;

CREATE TABLE posts (
    id                 UUID PRIMARY KEY,
    forum_id           UUID   NOT NULL REFERENCES forums (id) ON DELETE CASCADE,
    owner_id           BIGINT NOT NULL REFERENCES users (user_id),
    title              VARCHAR(300) NOT NULL,
    content            TEXT,
    upvote_count       INTEGER NOT NULL DEFAULT 0,    -- trigger-maintained (post_votes)
    downvote_count     INTEGER NOT NULL DEFAULT 0,    -- trigger-maintained (post_votes)
    score              INTEGER GENERATED ALWAYS AS (upvote_count - downvote_count) STORED,
    comment_count      INTEGER NOT NULL DEFAULT 0,    -- trigger-maintained (comments, is_deleted-aware)
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_activity_at   TIMESTAMPTZ,
    moderation_status  VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                         CHECK (moderation_status IN ('PENDING','APPROVED','REMOVED')),
    moderation_version BIGINT NOT NULL DEFAULT 1,
    is_deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at         TIMESTAMPTZ
);
CREATE INDEX idx_post_forum_new   ON posts (forum_id, created_at       DESC) WHERE NOT is_deleted;
CREATE INDEX idx_post_forum_score ON posts (forum_id, score            DESC) WHERE NOT is_deleted;
CREATE INDEX idx_post_forum_hot   ON posts (forum_id, last_activity_at DESC) WHERE NOT is_deleted;
CREATE INDEX idx_post_explore_new   ON posts (created_at DESC)             WHERE NOT is_deleted;
CREATE INDEX idx_post_explore_score ON posts (score DESC, created_at DESC) WHERE NOT is_deleted;
CREATE INDEX idx_post_owner ON posts (owner_id, created_at DESC) WHERE NOT is_deleted;

CREATE TABLE comments (
    id                 UUID PRIMARY KEY,
    post_id            UUID   NOT NULL REFERENCES posts (id)    ON DELETE CASCADE,
    parent_id          UUID   REFERENCES comments (id)          ON DELETE CASCADE,
    owner_id           BIGINT NOT NULL REFERENCES users (user_id),
    content            TEXT NOT NULL,
    upvote_count       INTEGER NOT NULL DEFAULT 0,    -- trigger-maintained (comment_votes)
    downvote_count     INTEGER NOT NULL DEFAULT 0,    -- trigger-maintained (comment_votes)
    score              INTEGER GENERATED ALWAYS AS (upvote_count - downvote_count) STORED,
    depth              INTEGER NOT NULL DEFAULT 0,
    number_of_replies  INTEGER NOT NULL DEFAULT 0,    -- trigger-maintained (direct children)
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    moderation_status  VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                         CHECK (moderation_status IN ('PENDING','APPROVED','REMOVED')),
    moderation_version BIGINT NOT NULL DEFAULT 1,
    is_deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at         TIMESTAMPTZ
);
CREATE INDEX idx_comment_post_new   ON comments (post_id, created_at) WHERE NOT is_deleted;
CREATE INDEX idx_comment_post_score ON comments (post_id, score DESC) WHERE NOT is_deleted;
CREATE INDEX idx_comment_parent     ON comments (parent_id, created_at) WHERE NOT is_deleted;

-- Votes split into two typed tables (was one polymorphic Mongo collection).
CREATE TABLE post_votes (
    user_id    BIGINT   NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    post_id    UUID     NOT NULL REFERENCES posts (id)      ON DELETE CASCADE,
    vote_type  INTEGER  NOT NULL CHECK (vote_type IN (-1, 1)),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, post_id)
);
CREATE INDEX idx_post_votes_post ON post_votes (post_id);

CREATE TABLE comment_votes (
    user_id    BIGINT   NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    comment_id UUID     NOT NULL REFERENCES comments (id)   ON DELETE CASCADE,
    vote_type  INTEGER  NOT NULL CHECK (vote_type IN (-1, 1)),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, comment_id)
);
CREATE INDEX idx_comment_votes_comment ON comment_votes (comment_id);

-- user → forum follows (was `following`). Hard-deleted; natural (user, forum) key.
CREATE TABLE forum_follows (
    user_id    BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    forum_id   UUID   NOT NULL REFERENCES forums (id)     ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, forum_id)
);
CREATE INDEX idx_forum_follows_user  ON forum_follows (user_id, created_at DESC);
CREATE INDEX idx_forum_follows_forum ON forum_follows (forum_id);

-- ─── Moderation transactional outbox (was a Mongo collection) ─────────────────
CREATE TABLE moderation_outbox (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- monotonic → per-content order
    content_type    VARCHAR(16) NOT NULL CHECK (content_type IN ('POST','COMMENT','FORUM')),
    content_id      UUID    NOT NULL,
    content_version BIGINT  NOT NULL,
    text            TEXT    NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =============================================================================
-- Counter-maintenance triggers for the social counters Mongo used to maintain in
-- app code. Base columns only (never the GENERATED score, which recomputes).
-- =============================================================================

-- forum_follows → forums.follower_count (hard delete only)
CREATE FUNCTION trg_forum_follow_counts() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE forums SET follower_count = follower_count + 1 WHERE id = NEW.forum_id;
    ELSIF TG_OP = 'DELETE' THEN
        -- is_deleted guard (see post_votes trigger): don't touch a forum being purged.
        UPDATE forums SET follower_count = follower_count - 1 WHERE id = OLD.forum_id AND is_deleted = false;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER forum_follow_counts AFTER INSERT OR DELETE ON forum_follows
    FOR EACH ROW EXECUTE FUNCTION trg_forum_follow_counts();

-- post_votes → posts.upvote_count / downvote_count (hard delete / upsert)
CREATE FUNCTION trg_post_vote_counts() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.vote_type = 1 THEN UPDATE posts SET upvote_count   = upvote_count   + 1 WHERE id = NEW.post_id;
                             ELSE UPDATE posts SET downvote_count = downvote_count + 1 WHERE id = NEW.post_id; END IF;
    ELSIF TG_OP = 'DELETE' THEN
        -- is_deleted guard: during a purge (DELETE FROM posts) the FK cascade deletes these
        -- votes and this trigger must NOT touch the post row the same command is deleting.
        IF OLD.vote_type = 1 THEN UPDATE posts SET upvote_count   = upvote_count   - 1 WHERE id = OLD.post_id AND is_deleted = false;
                             ELSE UPDATE posts SET downvote_count = downvote_count - 1 WHERE id = OLD.post_id AND is_deleted = false; END IF;
    ELSIF TG_OP = 'UPDATE' AND NEW.vote_type <> OLD.vote_type THEN
        IF NEW.vote_type = 1 THEN UPDATE posts SET upvote_count = upvote_count + 1, downvote_count = downvote_count - 1 WHERE id = NEW.post_id;
                             ELSE UPDATE posts SET upvote_count = upvote_count - 1, downvote_count = downvote_count + 1 WHERE id = NEW.post_id; END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER post_vote_counts AFTER INSERT OR UPDATE OR DELETE ON post_votes
    FOR EACH ROW EXECUTE FUNCTION trg_post_vote_counts();

-- comment_votes → comments.upvote_count / downvote_count
CREATE FUNCTION trg_comment_vote_counts() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.vote_type = 1 THEN UPDATE comments SET upvote_count   = upvote_count   + 1 WHERE id = NEW.comment_id;
                             ELSE UPDATE comments SET downvote_count = downvote_count + 1 WHERE id = NEW.comment_id; END IF;
    ELSIF TG_OP = 'DELETE' THEN
        -- is_deleted guard (see post_votes trigger): don't touch a comment being purged.
        IF OLD.vote_type = 1 THEN UPDATE comments SET upvote_count   = upvote_count   - 1 WHERE id = OLD.comment_id AND is_deleted = false;
                             ELSE UPDATE comments SET downvote_count = downvote_count - 1 WHERE id = OLD.comment_id AND is_deleted = false; END IF;
    ELSIF TG_OP = 'UPDATE' AND NEW.vote_type <> OLD.vote_type THEN
        IF NEW.vote_type = 1 THEN UPDATE comments SET upvote_count = upvote_count + 1, downvote_count = downvote_count - 1 WHERE id = NEW.comment_id;
                             ELSE UPDATE comments SET upvote_count = upvote_count - 1, downvote_count = downvote_count + 1 WHERE id = NEW.comment_id; END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER comment_vote_counts AFTER INSERT OR UPDATE OR DELETE ON comment_votes
    FOR EACH ROW EXECUTE FUNCTION trg_comment_vote_counts();

-- posts → forums.post_count. is_deleted-aware: counts while NOT is_deleted, so a
-- soft-delete decrements and the eventual hard purge does not double-count.
CREATE FUNCTION trg_forum_post_count() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NOT NEW.is_deleted THEN UPDATE forums SET post_count = post_count + 1 WHERE id = NEW.forum_id; END IF;
    ELSIF TG_OP = 'DELETE' THEN
        IF NOT OLD.is_deleted THEN UPDATE forums SET post_count = post_count - 1 WHERE id = OLD.forum_id; END IF;
    ELSIF TG_OP = 'UPDATE' AND OLD.is_deleted <> NEW.is_deleted THEN
        IF NEW.is_deleted THEN UPDATE forums SET post_count = post_count - 1 WHERE id = NEW.forum_id;
                          ELSE UPDATE forums SET post_count = post_count + 1 WHERE id = NEW.forum_id; END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER forum_post_count AFTER INSERT OR UPDATE OR DELETE ON posts
    FOR EACH ROW EXECUTE FUNCTION trg_forum_post_count();

-- comments → posts.comment_count (is_deleted-aware; post_id is immutable). Writes a
-- DIFFERENT table than the triggering one, so it is safe under bulk soft-deletes.
-- number_of_replies is deliberately NOT maintained here: a comments→comments trigger
-- would update a parent row that a subtree/forum/post bulk soft-delete has in the same
-- statement ("tuple already modified" error). It is maintained in CommentService via
-- atomic @Modifying increments instead.
CREATE FUNCTION trg_comment_counts() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NOT NEW.is_deleted THEN
            UPDATE posts SET comment_count = comment_count + 1 WHERE id = NEW.post_id;
        END IF;
    ELSIF TG_OP = 'DELETE' THEN
        IF NOT OLD.is_deleted THEN
            UPDATE posts SET comment_count = comment_count - 1 WHERE id = OLD.post_id;
        END IF;
    ELSIF TG_OP = 'UPDATE' AND OLD.is_deleted <> NEW.is_deleted THEN
        IF NEW.is_deleted THEN
            UPDATE posts SET comment_count = comment_count - 1 WHERE id = NEW.post_id;
        ELSE
            UPDATE posts SET comment_count = comment_count + 1 WHERE id = NEW.post_id;
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER comment_counts AFTER INSERT OR UPDATE OR DELETE ON comments
    FOR EACH ROW EXECUTE FUNCTION trg_comment_counts();
