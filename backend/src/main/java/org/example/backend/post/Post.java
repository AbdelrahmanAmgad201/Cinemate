package org.example.backend.post;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.example.backend.common.SoftDeletable;
import org.example.backend.moderation.Moderatable;
import org.example.backend.moderation.ModerationStatus;
import org.example.backend.util.UuidV7;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "posts")
public class Post extends SoftDeletable implements Moderatable {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "forum_id", nullable = false)
    private UUID forumId;

    // Real FK to users(user_id). forum_name / author_name denormalization removed —
    // the view queries join to forums / users instead (no staleness to maintain).
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    // Trigger-maintained (post_votes); updatable=false so a title edit can't clobber them.
    @Column(name = "upvote_count", nullable = false, updatable = false)
    @Builder.Default
    private Integer upvoteCount = 0;

    @Column(name = "downvote_count", nullable = false, updatable = false)
    @Builder.Default
    private Integer downvoteCount = 0;

    // Generated column (upvote_count - downvote_count) — read-only from the app.
    @Column(name = "score", insertable = false, updatable = false)
    private Integer score;

    // Trigger-maintained (comments, is_deleted-aware).
    @Column(name = "comment_count", nullable = false, updatable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    @Builder.Default
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    @Column(name = "moderation_version", nullable = false)
    @Builder.Default
    private long moderationVersion = 1;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UuidV7.generate();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastActivityAt == null) {
            lastActivityAt = createdAt;
        }
    }
}
