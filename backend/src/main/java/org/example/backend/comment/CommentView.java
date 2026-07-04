package org.example.backend.comment;

import java.time.Instant;

/**
 * Comment list/detail view (CQ-NEW-03) — returned instead of the {@link Comment}
 * document itself. Spring Data populates this via projection, no manual mapping needed.
 */
public interface CommentView {
    String getId();
    String getPostId();
    String getParentId();
    String getOwnerId();
    String getContent();
    Integer getUpvoteCount();
    Integer getDownvoteCount();
    Integer getScore();
    Integer getDepth();
    Instant getCreatedAt();
    Integer getNumberOfReplies();
    Boolean getIsDeleted();
}
