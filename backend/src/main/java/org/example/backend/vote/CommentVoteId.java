package org.example.backend.vote;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite key for {@link CommentVote}: one vote per (user, comment). */
public class CommentVoteId implements Serializable {

    private Long userId;
    private UUID commentId;

    public CommentVoteId() {
    }

    public CommentVoteId(Long userId, UUID commentId) {
        this.userId = userId;
        this.commentId = commentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentVoteId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(commentId, that.commentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, commentId);
    }
}
