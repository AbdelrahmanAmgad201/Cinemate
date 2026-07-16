package org.example.backend.forum;

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
@Table(name = "forums")
public class Forum extends SoftDeletable implements Moderatable {

    @Id
    @Column(name = "id")
    private UUID id;

    // Real FK to users(user_id) now (was a fabricated ObjectId via IdConverter).
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    // Trigger-maintained (forum_follows) — never written by the app, so updatable=false
    // stops a full-entity UPDATE from clobbering the trigger's value.
    @Column(name = "follower_count", nullable = false, updatable = false)
    @Builder.Default
    private Integer followerCount = 0;

    // Trigger-maintained (posts, is_deleted-aware).
    @Column(name = "post_count", nullable = false, updatable = false)
    @Builder.Default
    private Integer postCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Optimistic-publish moderation state. name + description are moderated together.
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    @Builder.Default
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    @Column(name = "moderation_version", nullable = false)
    @Builder.Default
    private long moderationVersion = 1;

    // When the CURRENT moderation request was made (distinct from createdAt, which never
    // changes on edit). Drives the MOD-01 reconciliation sweep: PENDING content whose
    // request is older than the sweep threshold is re-enqueued.
    @Column(name = "moderation_requested_at", nullable = false)
    private Instant moderationRequestedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UuidV7.generate();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (moderationRequestedAt == null) {
            moderationRequestedAt = createdAt;
        }
    }
}
