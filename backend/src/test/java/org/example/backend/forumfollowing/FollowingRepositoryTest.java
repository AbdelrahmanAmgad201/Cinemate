package org.example.backend.forumfollowing;

import org.bson.types.ObjectId;
import org.example.backend.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FollowingRepositoryTest extends AbstractMongoIntegrationTest {

    @Autowired
    private FollowingRepository followingRepository;

    @BeforeEach
    void setUp() {
        followingRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRetrieveFollowing() {
        // Arrange
        ObjectId userId = new ObjectId();
        ObjectId forumId = new ObjectId();

        Following following = Following.builder()
                .id(new ObjectId())
                .userId(userId)
                .forumId(forumId)
                .createdAt(Instant.now())
                .build();

        // Act
        followingRepository.save(following);
        Following saved = followingRepository.findById(following.getId()).orElse(null);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getForumId()).isEqualTo(forumId);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindFollowingByUserId() {
        // Arrange
        ObjectId userId = new ObjectId();
        ObjectId forumId1 = new ObjectId();
        ObjectId forumId2 = new ObjectId();
        ObjectId otherUserId = new ObjectId();

        Following following1 = Following.builder()
                .id(new ObjectId())
                .userId(userId)
                .forumId(forumId1)
                .createdAt(Instant.now())
                .build();

        Following following2 = Following.builder()
                .id(new ObjectId())
                .userId(userId)
                .forumId(forumId2)
                .createdAt(Instant.now())
                .build();

        Following following3 = Following.builder()
                .id(new ObjectId())
                .userId(otherUserId)
                .forumId(forumId1)
                .createdAt(Instant.now())
                .build();

        followingRepository.save(following1);
        followingRepository.save(following2);
        followingRepository.save(following3);

        // Act
        List<Following> userFollowings = followingRepository.findByUserId(userId);

        // Assert
        assertThat(userFollowings).hasSize(2);
        assertThat(userFollowings)
                .extracting(Following::getUserId)
                .containsOnly(userId);
        assertThat(userFollowings)
                .extracting(Following::getForumId)
                .containsExactlyInAnyOrder(forumId1, forumId2);
    }

    @Test
    void shouldReturnEmptyListWhenNoFollowingsExist() {
        // Arrange
        ObjectId userId = new ObjectId();

        // Act
        List<Following> userFollowings = followingRepository.findByUserId(userId);

        // Assert
        assertThat(userFollowings).isEmpty();
    }


    @Test
    void shouldDeleteFollowing() {
        // Arrange
        ObjectId userId = new ObjectId();
        ObjectId forumId = new ObjectId();

        Following following = Following.builder()
                .id(new ObjectId())
                .userId(userId)
                .forumId(forumId)
                .createdAt(Instant.now())
                .build();

        followingRepository.save(following);

        // Act
        followingRepository.delete(following);
        Following deleted = followingRepository.findById(following.getId()).orElse(null);

        // Assert
        assertThat(deleted).isNull();
    }
}