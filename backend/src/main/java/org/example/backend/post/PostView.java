package org.example.backend.post;

import java.time.Instant;

public interface PostView {
    String getId();
    String getForumId();
    String getOwnerId();
    String getForumName();
    String getAuthorName();
    String getTitle();
    String getContent();
    Integer getCommentCount();
    Integer getUpvoteCount();
    Integer getDownvoteCount();
    Integer getScore();
    Instant getCreatedAt();
}
