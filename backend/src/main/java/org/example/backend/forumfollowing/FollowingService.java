package org.example.backend.forumfollowing;

import lombok.RequiredArgsConstructor;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Following forums. Hard-delete now (unfollow removes the row); forum.follower_count is
 * maintained by a DB trigger, so this service no longer touches it.
 */
@Service
@RequiredArgsConstructor
public class FollowingService {

    private final FollowingRepository followingRepository;
    private final ForumRepository forumRepository;

    @Transactional
    public void follow(UUID forumId, Long userId) {
        if (followingRepository.existsByUserIdAndForumId(userId, forumId)) {
            return;
        }
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new IllegalArgumentException("Forum not found"));
        if (Boolean.TRUE.equals(forum.getIsDeleted())) {
            throw new IllegalStateException("Cannot follow a deleted forum");
        }
        followingRepository.save(Following.builder()
                .userId(userId)
                .forumId(forumId)
                .createdAt(Instant.now())
                .build());
    }

    @Transactional
    public void unfollow(UUID forumId, Long userId) {
        followingRepository.findByUserIdAndForumId(userId, forumId)
                .ifPresent(followingRepository::delete);
    }

    @Transactional(readOnly = true)
    public ForumPageResponse getFollowedForums(Long userId, Pageable pageable) {
        Page<Following> followingPage = followingRepository.findByUserId(userId, pageable);
        List<UUID> forumIds = followingPage.getContent().stream()
                .map(Following::getForumId)
                .collect(Collectors.toList());

        List<Forum> forums = forumIds.isEmpty()
                ? List.of()
                : forumRepository.findAllByIdInAndIsDeletedFalse(forumIds);

        return ForumPageResponse.builder()
                .forums(forums)
                .currentPage(followingPage.getNumber())
                .totalPages(followingPage.getTotalPages())
                .totalElements(followingPage.getTotalElements())
                .pageSize(followingPage.getSize())
                .hasNext(followingPage.hasNext())
                .hasPrevious(followingPage.hasPrevious())
                .build();
    }

    @Transactional(readOnly = true)
    public Boolean isFollowed(UUID forumId, Long userId) {
        return followingRepository.existsByUserIdAndForumId(userId, forumId);
    }
}
