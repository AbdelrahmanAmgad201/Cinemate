package org.example.backend.vote;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A user's vote on a post. Split from the old polymorphic {@code Vote} document so the
 * FK, one-vote-per-user PK, and count trigger are all real and simple. Hard-deleted /
 * upserted: "change vote" flips {@code voteType}, "remove vote" deletes the row.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "post_votes")
@IdClass(PostVoteId.class)
public class PostVote {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "post_id")
    private UUID postId;

    @Column(name = "vote_type", nullable = false)
    private Integer voteType;   // 1 = upvote, -1 = downvote

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
