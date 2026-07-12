package org.example.backend.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findByForumId(UUID forumId);

    // PostView (not Post) so the controller never serializes the raw entity (CQ-NEW-03).
    Optional<PostView> findByIdAndIsDeletedFalse(UUID id);

    Page<Post> findByIsDeletedFalseAndCreatedAtGreaterThanEqual(Instant since, Pageable pageable);

    Page<PostView> findByIsDeletedFalseAndForumIdIn(List<UUID> forumIds, Pageable pageable);
    Page<PostView> findByIsDeletedFalseAndForumId(UUID forumId, Pageable pageable);
    Page<PostView> findAllByOwnerIdAndIsDeletedFalse(Long ownerId, Pageable pageable);

    @Modifying
    @Query("update Post p set p.isDeleted = true, p.deletedAt = :ts where p.id = :id and p.isDeleted = false")
    int softDelete(@Param("id") UUID id, @Param("ts") Instant ts);

    @Modifying
    @Query("update Post p set p.isDeleted = true, p.deletedAt = :ts where p.forumId = :forumId and p.isDeleted = false")
    int softDeleteByForum(@Param("forumId") UUID forumId, @Param("ts") Instant ts);

    @Modifying
    @Query("update Post p set p.lastActivityAt = :ts where p.id = :id")
    int touchLastActivity(@Param("id") UUID id, @Param("ts") Instant ts);

    @Modifying
    @Query("delete from Post p where p.isDeleted = true and p.deletedAt < :cutoff")
    int purgeDeletedBefore(@Param("cutoff") Instant cutoff);

    // Moderation-status changes via bulk update (never a managed entity — see
    // ModerationVerdictConsumer: mixing an entity dirty-flush with a bulk soft-delete on
    // the same row would resurrect the row's is_deleted on flush).
    @Modifying
    @Query("update Post p set p.moderationStatus = org.example.backend.moderation.ModerationStatus.APPROVED " +
           "where p.id = :id and p.moderationVersion = :version " +
           "and p.moderationStatus = org.example.backend.moderation.ModerationStatus.PENDING")
    int approveModeration(@Param("id") UUID id, @Param("version") long version);

    @Modifying
    @Query("update Post p set p.moderationStatus = org.example.backend.moderation.ModerationStatus.REMOVED where p.id = :id")
    int markModerationRemoved(@Param("id") UUID id);
}
