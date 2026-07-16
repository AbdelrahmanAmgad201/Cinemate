-- movie_review.created_at was a naive TIMESTAMP (no timezone). Postgres stores naive
-- timestamps as-is and the JVM wrote them via LocalDateTime.now() in the container's
-- zone (UTC), but Jackson then serialized them with no 'Z'/offset suffix, so browsers
-- parsed them as local time — a movie review posted seconds ago rendered as "N hours
-- ago" for any client not in UTC. Posts/comments already use Instant/timestamptz and
-- are unaffected. Converting to TIMESTAMPTZ (interpreting existing values as UTC, which
-- is what LocalDateTime.now() actually produced) fixes this at the source.
ALTER TABLE movie_review
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
