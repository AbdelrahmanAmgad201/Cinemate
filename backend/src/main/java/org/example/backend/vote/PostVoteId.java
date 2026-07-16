package org.example.backend.vote;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite key for {@link PostVote}: one vote per (user, post). */
public class PostVoteId implements Serializable {

    private Long userId;
    private UUID postId;

    public PostVoteId() {
    }

    public PostVoteId(Long userId, UUID postId) {
        this.userId = userId;
        this.postId = postId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostVoteId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(postId, that.postId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, postId);
    }
}
