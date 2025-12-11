package org.example.backend.forumfollowing;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowingService {
    private final FollowingRepository followingRepository;
    private final ForumRepository forumRepository;
    private final MongoTemplate mongoTemplate;

    public void follow(ObjectId forumId, Long userId) {
        ObjectId userObjectId = longToObjectId(userId);

        if (followingRepository.existsByUserIdAndForumId(userObjectId, forumId)) {
            return;
        }

        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new IllegalArgumentException("Forum not found"));

        if (Boolean.TRUE.equals(forum.getIsDeleted())) {
            throw new IllegalStateException("Cannot follow a deleted forum");
        }

        Following following = Following.builder()
                .userId(userObjectId)
                .forumId(forumId)
                .createdAt(Instant.now())
                .build();
        followingRepository.save(following);

        // Increment follower count
        forum.setFollowerCount(forum.getFollowerCount() + 1);
        forumRepository.save(forum);
    }

    public void unfollow(ObjectId forumId, Long userId) {
        ObjectId userObjectId = longToObjectId(userId);

        if (!followingRepository.existsByUserIdAndForumId(userObjectId, forumId)) {
            return;
        }

        followingRepository.deleteByUserIdAndForumId(userObjectId, forumId);

        // Decrement follower count
        forumRepository.findById(forumId).ifPresent(forum -> {
            forum.setFollowerCount(Math.max(0, forum.getFollowerCount() - 1));
            forumRepository.save(forum);
        });
    }

    public ForumPageResponse getFollowedForums(Long userId, Pageable pageable) {
        ObjectId userObjectId = longToObjectId(userId);


        Page<Following> followingPage = followingRepository.findByUserId(userObjectId, pageable);
        List<ObjectId> forumIds = followingPage.getContent()
                .stream()
                .map(Following::getForumId)
                .collect(Collectors.toList());

        List<Forum> forums = fetchNonDeletedForums(forumIds);

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

    public Boolean isFollowed(ObjectId forumId, Long userId) {
        ObjectId userObjectId = longToObjectId(userId);
        return followingRepository.existsByUserIdAndForumId(userObjectId, forumId);
    }

    private List<Forum> fetchNonDeletedForums(List<ObjectId> forumIds) {
        if (forumIds.isEmpty()) {
            return List.of();
        }

        Query query = new Query(
                Criteria.where("_id").in(forumIds)
                        .and("isDeleted").is(false)
        );

        return mongoTemplate.find(query, Forum.class);
    }

    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}