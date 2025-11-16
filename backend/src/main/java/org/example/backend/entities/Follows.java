package org.example.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "follows", indexes = {
        @Index(name = "idx_followed_user", columnList = "followed_user_id")
})
public class Follows {
    @EmbeddedId
    private FollowsID followsID;

    @ManyToOne
    @MapsId("followingUserId")
    @JoinColumn(name = "following_user_id", nullable = false)
    private User followingUser;

    @ManyToOne
    @MapsId("followedUserId")
    @JoinColumn(name = "followed_user_id", nullable = false)
    private User followedUser;

    @Column(name = "followed_at", nullable = false, updatable = false)
    private LocalDateTime followedAt;

    @PrePersist
    protected void onCreate() {
        if (followedAt == null) {
            followedAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Follows)) return false;
        Follows follows = (Follows) o;
        return followsID != null && followsID.equals(follows.followsID);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}