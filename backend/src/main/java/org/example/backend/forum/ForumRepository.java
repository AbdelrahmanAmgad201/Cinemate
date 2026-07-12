package org.example.backend.forum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ForumRepository extends JpaRepository<Forum, UUID> {
    List<Forum> findByOwnerId(Long ownerId);
    Page<Forum> findByNameContainingIgnoreCaseAndIsDeletedFalse(String name, Pageable pageable);
    Page<Forum> findAllByIsDeletedFalse(Pageable pageable);
    Page<ForumDisplayDTO> findAllByOwnerIdAndIsDeletedFalse(Long ownerId, Pageable pageable);
    List<Forum> findAllByIdInAndIsDeletedFalse(List<UUID> ids);

    @Modifying
    @Query("update Forum f set f.isDeleted = true, f.deletedAt = :ts where f.id = :id and f.isDeleted = false")
    int softDelete(@Param("id") UUID id, @Param("ts") Instant ts);

    // Physical purge; FK ON DELETE CASCADE removes posts/comments/votes/follows.
    @Modifying
    @Query("delete from Forum f where f.isDeleted = true and f.deletedAt < :cutoff")
    int purgeDeletedBefore(@Param("cutoff") Instant cutoff);

    // Moderation-status changes via bulk update (never a managed entity — see PostRepository).
    @Modifying
    @Query("update Forum f set f.moderationStatus = org.example.backend.moderation.ModerationStatus.APPROVED " +
           "where f.id = :id and f.moderationVersion = :version " +
           "and f.moderationStatus = org.example.backend.moderation.ModerationStatus.PENDING")
    int approveModeration(@Param("id") UUID id, @Param("version") long version);

    @Modifying
    @Query("update Forum f set f.moderationStatus = org.example.backend.moderation.ModerationStatus.REMOVED where f.id = :id")
    int markModerationRemoved(@Param("id") UUID id);
}
