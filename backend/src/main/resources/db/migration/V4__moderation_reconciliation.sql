-- MOD-01: reconciliation sweep for content stuck in PENDING (verdict never arrived).
--
-- moderation_requested_at tracks when the CURRENT moderation request was made, separate
-- from created_at (which never changes on edit). The sweep re-enqueues PENDING content
-- whose request is older than a configurable threshold, so a dropped/lost verdict doesn't
-- leave content unmoderated forever.

ALTER TABLE forums   ADD COLUMN moderation_requested_at TIMESTAMPTZ;
ALTER TABLE posts    ADD COLUMN moderation_requested_at TIMESTAMPTZ;
ALTER TABLE comments ADD COLUMN moderation_requested_at TIMESTAMPTZ;

UPDATE forums   SET moderation_requested_at = created_at WHERE moderation_requested_at IS NULL;
UPDATE posts    SET moderation_requested_at = created_at WHERE moderation_requested_at IS NULL;
UPDATE comments SET moderation_requested_at = created_at WHERE moderation_requested_at IS NULL;

ALTER TABLE forums   ALTER COLUMN moderation_requested_at SET NOT NULL;
ALTER TABLE posts    ALTER COLUMN moderation_requested_at SET NOT NULL;
ALTER TABLE comments ALTER COLUMN moderation_requested_at SET NOT NULL;

-- Sweep query is "PENDING AND requested_at < cutoff"; partial index keeps it cheap even
-- as approved/removed content comes to dominate each table over time.
CREATE INDEX idx_forum_pending_sweep   ON forums   (moderation_requested_at) WHERE moderation_status = 'PENDING';
CREATE INDEX idx_post_pending_sweep    ON posts    (moderation_requested_at) WHERE moderation_status = 'PENDING';
CREATE INDEX idx_comment_pending_sweep ON comments (moderation_requested_at) WHERE moderation_status = 'PENDING';
