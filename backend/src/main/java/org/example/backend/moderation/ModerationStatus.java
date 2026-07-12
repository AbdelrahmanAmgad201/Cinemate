package org.example.backend.moderation;

/**
 * Moderation bookkeeping on content documents (optimistic-publish model: content
 * is visible in every state — a REMOVED verdict soft-deletes it retroactively).
 * Legacy documents created before this field existed have {@code null}, which is
 * treated like APPROVED (they were moderated under the old synchronous model).
 */
public enum ModerationStatus {
    /** Published and visible; the async verdict hasn't landed yet. */
    PENDING,
    /** A clean verdict landed for the current version. */
    APPROVED,
    /** A flagged verdict landed; the content has been soft-deleted. */
    REMOVED
}
