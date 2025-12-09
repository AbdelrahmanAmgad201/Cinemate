package org.example.backend.forumfollowing;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FollowingService {
    private final FollowingRepository followingRepository;
    private final ForumRepository forumRepository;

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
    }

    public void unfollow(ObjectId forumId, Long userId) {
        ObjectId userObjectId = longToObjectId(userId);
        followingRepository.deleteByUserIdAndForumId(userObjectId, forumId);
    }

    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}
