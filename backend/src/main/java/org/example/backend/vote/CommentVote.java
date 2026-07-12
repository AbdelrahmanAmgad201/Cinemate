package org.example.backend.vote;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** A user's vote on a comment. See {@link PostVote} for the design rationale. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "comment_votes")
@IdClass(CommentVoteId.class)
public class CommentVote {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "comment_id")
    private UUID commentId;

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
