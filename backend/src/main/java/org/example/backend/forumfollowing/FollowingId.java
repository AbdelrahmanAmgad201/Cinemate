package org.example.backend.forumfollowing;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite key for {@link Following} (forum_follows): one follow per (user, forum). */
public class FollowingId implements Serializable {

    private Long userId;
    private UUID forumId;

    public FollowingId() {
    }

    public FollowingId(Long userId, UUID forumId) {
        this.userId = userId;
        this.forumId = forumId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowingId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(forumId, that.forumId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, forumId);
    }
}
