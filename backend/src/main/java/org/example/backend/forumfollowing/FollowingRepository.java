package org.example.backend.forumfollowing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowingRepository extends JpaRepository<Following, FollowingId> {
    // forum_follows is hard-deleted now (no isDeleted filter): unfollow removes the row,
    // follow inserts it. The natural (user, forum) PK makes both idempotent-safe.
    Optional<Following> findByUserIdAndForumId(Long userId, UUID forumId);

    boolean existsByUserIdAndForumId(Long userId, UUID forumId);

    List<Following> findByUserId(Long userId);

    Page<Following> findByUserId(Long userId, Pageable pageable);

    @Query("select f.forumId from Following f where f.userId = :userId")
    List<UUID> findForumIdsByUserId(@Param("userId") Long userId);
}
