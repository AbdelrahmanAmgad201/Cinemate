package org.example.backend.comment;

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
@Table(name = "comments")
public class Comment extends SoftDeletable implements Moderatable {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    // null for top-level comments; self-FK with ON DELETE CASCADE in the schema.
    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    // forum_id / post_owner_id denormalization removed — access checks join
    // comments -> posts -> forums (cheap on PK/FK).

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "upvote_count", nullable = false, updatable = false)
    @Builder.Default
    private Integer upvoteCount = 0;

    @Column(name = "downvote_count", nullable = false, updatable = false)
    @Builder.Default
    private Integer downvoteCount = 0;

    @Column(name = "score", insertable = false, updatable = false)
    private Integer score;

    @Column(nullable = false)
    @Builder.Default
    private Integer depth = 0;   // 0 = top-level, 1 = reply, 2 = nested reply

    // Direct-children count. Maintained by CommentService via atomic @Modifying increments
    // (not a trigger — a comments→comments trigger collides with bulk subtree soft-deletes).
    // updatable=false so an entity flush can't clobber those explicit JPQL updates.
    @Column(name = "number_of_replies", nullable = false, updatable = false)
    @Builder.Default
    private Integer numberOfReplies = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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
