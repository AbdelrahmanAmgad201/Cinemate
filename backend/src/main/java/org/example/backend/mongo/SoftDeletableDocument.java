package org.example.backend.mongo;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Shared soft-delete fields for MongoDB documents (DB-NEW-03). Spring Data MongoDB has
 * no {@code @MappedSuperclass} equivalent — plain Java inheritance is enough, since the
 * mapping converter flattens inherited fields onto the same document as the subclass's
 * own fields, under their declared names ("isDeleted"/"deletedAt"), with no nesting.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class SoftDeletableDocument {

    @Builder.Default
    private Boolean isDeleted = false;

    private Instant deletedAt;
}
