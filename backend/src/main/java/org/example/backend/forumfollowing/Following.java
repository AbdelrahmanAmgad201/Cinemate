package org.example.backend.forumfollowing;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A user following a forum (table {@code forum_follows}). Hard-deleted now: unfollow
 * removes the row, follow inserts it (the natural (user, forum) PK makes re-follow a
 * plain insert). The old Mongo soft-delete + "reactivate in place" only existed to
 * dodge a unique index — gone.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "forum_follows")
@IdClass(FollowingId.class)
public class Following {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "forum_id")
    private UUID forumId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
