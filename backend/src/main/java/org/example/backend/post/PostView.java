package org.example.backend.post;

import java.time.Instant;
import java.util.UUID;

/**
 * Post list/detail projection (CQ-NEW-03) — returned instead of the {@link Post} entity.
 * forumName/authorName are no longer denormalized onto the post; the frontend resolves
 * those via the forum-name and user-name endpoints, so they're not part of this view.
 */
public interface PostView {
    UUID getId();
    UUID getForumId();
    Long getOwnerId();
    String getTitle();
    String getContent();
    Integer getCommentCount();
    Integer getUpvoteCount();
    Integer getDownvoteCount();
    Integer getScore();
    Instant getCreatedAt();
}
