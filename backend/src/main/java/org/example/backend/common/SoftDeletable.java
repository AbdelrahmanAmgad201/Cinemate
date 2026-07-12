package org.example.backend.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Shared soft-delete columns for content that is moderatable/restorable
 * (Forum, Post, Comment). Replaces the old Mongo {@code SoftDeletableDocument};
 * as a JPA {@code @MappedSuperclass} the columns land on the subclass's own table.
 *
 * <p>{@code isDeleted} is a {@code Boolean} (not primitive) so Lombok generates
 * {@code getIsDeleted()} — the name the {@link org.example.backend.moderation.Moderatable}
 * contract and the rest of the codebase already use.
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class SoftDeletable {

    @Column(name = "is_deleted", nullable = false)
    @lombok.Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
