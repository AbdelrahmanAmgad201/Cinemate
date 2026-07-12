package org.example.backend.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    // CommentView (not Comment) so the controller never serializes the raw entity (CQ-NEW-03).
    Page<CommentView> findByPostIdAndIsDeletedAndDepth(UUID postId, Boolean isDeleted, Integer depth, Pageable pageable);
    // Pageable (not Sort) caps replies at a fixed size (API-NEW-01) — one viral comment
    // could otherwise have an unbounded number of direct replies.
    List<CommentView> findByParentIdAndIsDeleted(UUID parentId, Boolean isDeleted, Pageable pageable);

    // Recursive subtree soft-delete (replaces the Mongo $graphLookup). Triggers keep
    // posts.comment_count and parents' number_of_replies correct as each row flips.
    @Modifying
    @Query(value = """
            WITH RECURSIVE subtree AS (
                SELECT id FROM comments WHERE id = :root
                UNION ALL
                SELECT c.id FROM comments c JOIN subtree s ON c.parent_id = s.id
            )
            UPDATE comments SET is_deleted = true, deleted_at = :ts
            WHERE id IN (SELECT id FROM subtree) AND is_deleted = false
            """, nativeQuery = true)
    int softDeleteSubtree(@Param("root") UUID root, @Param("ts") Instant ts);

    @Modifying
    @Query("update Comment c set c.isDeleted = true, c.deletedAt = :ts where c.postId = :postId and c.isDeleted = false")
    int softDeleteByPost(@Param("postId") UUID postId, @Param("ts") Instant ts);

    @Modifying
    @Query("update Comment c set c.isDeleted = true, c.deletedAt = :ts " +
           "where c.postId in (select p.id from Post p where p.forumId = :forumId) and c.isDeleted = false")
    int softDeleteByForum(@Param("forumId") UUID forumId, @Param("ts") Instant ts);

    @Modifying
    @Query("delete from Comment c where c.isDeleted = true and c.deletedAt < :cutoff")
    int purgeDeletedBefore(@Param("cutoff") Instant cutoff);

    // number_of_replies is maintained here (not in a trigger) with atomic increments, so
    // a comments→comments trigger can't collide with bulk subtree soft-deletes. Atomic +/-
    // is also concurrency-safe (two replies to the same parent can't lose an update).
    @Modifying
    @Query("update Comment c set c.numberOfReplies = c.numberOfReplies + 1 where c.id = :id")
    int incrementReplies(@Param("id") UUID id);

    @Modifying
    @Query("update Comment c set c.numberOfReplies = c.numberOfReplies - 1 where c.id = :id")
    int decrementReplies(@Param("id") UUID id);

    // Moderation-status changes via bulk update (never a managed entity — see PostRepository).
    @Modifying
    @Query("update Comment c set c.moderationStatus = org.example.backend.moderation.ModerationStatus.APPROVED " +
           "where c.id = :id and c.moderationVersion = :version " +
           "and c.moderationStatus = org.example.backend.moderation.ModerationStatus.PENDING")
    int approveModeration(@Param("id") UUID id, @Param("version") long version);

    @Modifying
    @Query("update Comment c set c.moderationStatus = org.example.backend.moderation.ModerationStatus.REMOVED where c.id = :id")
    int markModerationRemoved(@Param("id") UUID id);
}
