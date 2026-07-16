package org.example.backend.comment;

import java.time.Instant;
import java.util.UUID;

/**
 * Comment list/detail view (CQ-NEW-03) — returned instead of the {@link Comment}
 * entity. Spring Data populates this via projection, no manual mapping needed.
 */
public interface CommentView {
    UUID getId();
    UUID getPostId();
    UUID getParentId();
    Long getOwnerId();
    String getContent();
    Integer getUpvoteCount();
    Integer getDownvoteCount();
    Integer getScore();
    Integer getDepth();
    Instant getCreatedAt();
    Integer getNumberOfReplies();
    Boolean getIsDeleted();
}
