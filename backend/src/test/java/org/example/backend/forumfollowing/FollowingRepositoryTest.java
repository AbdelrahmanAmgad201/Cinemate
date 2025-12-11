package org.example.backend.forumfollowing;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.backend.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FollowingRepositoryTest extends AbstractMongoIntegrationTest {

    @Autowired
    private FollowingRepository followingRepository;

    private ObjectId userId;
    private ObjectId forumId1;
    private ObjectId forumId2;

    @BeforeEach
    void setUp() {
        userId = new ObjectId();
        forumId1 = new ObjectId();
        forumId2 = new ObjectId();
    }

    @Test
    void findByUserId_ReturnsAllFollowings() {
        Following following1 = createFollowing(userId, forumId1);
        Following following2 = createFollowing(userId, forumId2);
        followingRepository.saveAll(List.of(following1, following2));

        List<Following> result = followingRepository.findByUserId(userId);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(f -> f.getForumId().equals(forumId1)));
        assertTrue(result.stream().anyMatch(f -> f.getForumId().equals(forumId2)));
    }

    @Test
    void findByUserId_NoResults_ReturnsEmpty() {
        List<Following> result = followingRepository.findByUserId(new ObjectId());

        assertTrue(result.isEmpty());
    }

    @Test
    void existsByUserIdAndForumId_Exists_ReturnsTrue() {
        Following following = createFollowing(userId, forumId1);
        followingRepository.save(following);

        boolean exists = followingRepository.existsByUserIdAndForumId(userId, forumId1);

        assertTrue(exists);
    }

    @Test
    void existsByUserIdAndForumId_NotExists_ReturnsFalse() {
        boolean exists = followingRepository.existsByUserIdAndForumId(userId, forumId1);

        assertFalse(exists);
    }

    @Test
    void deleteByUserIdAndForumId_DeletesSuccessfully() {
        Following following = createFollowing(userId, forumId1);
        followingRepository.save(following);

        followingRepository.deleteByUserIdAndForumId(userId, forumId1);

        assertFalse(followingRepository.existsByUserIdAndForumId(userId, forumId1));
    }

    @Test
    void deleteByUserIdAndForumId_NotExists_NoError() {
        assertDoesNotThrow(() ->
                followingRepository.deleteByUserIdAndForumId(userId, forumId1));
    }

    @Test
    void findByUserId_WithPageable_ReturnsPaginatedResults() {
        Instant now = Instant.now();
        Following following1 = createFollowing(userId, forumId1, now.minus(2, ChronoUnit.DAYS));
        Following following2 = createFollowing(userId, forumId2, now.minus(1, ChronoUnit.DAYS));
        Following following3 = createFollowing(userId, new ObjectId(), now);
        followingRepository.saveAll(List.of(following1, following2, following3));

        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Following> page = followingRepository.findByUserId(userId, pageRequest);

        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertTrue(page.hasNext());
        assertFalse(page.hasPrevious());
        // Should be sorted by createdAt DESC, so most recent first
        assertEquals(following3.getForumId(), page.getContent().get(0).getForumId());
    }

    @Test
    void findByUserId_WithPageable_SecondPage() {
        Following following1 = createFollowing(userId, forumId1);
        Following following2 = createFollowing(userId, forumId2);
        Following following3 = createFollowing(userId, new ObjectId());
        followingRepository.saveAll(List.of(following1, following2, following3));

        PageRequest pageRequest = PageRequest.of(1, 2);
        Page<Following> page = followingRepository.findByUserId(userId, pageRequest);

        assertEquals(1, page.getContent().size());
        assertFalse(page.hasNext());
        assertTrue(page.hasPrevious());
    }

    @Test
    void findForumIdsByUserId_ReturnsOnlyForumIds() {
        Following following1 = createFollowing(userId, forumId1);
        Following following2 = createFollowing(userId, forumId2);
        followingRepository.saveAll(List.of(following1, following2));

        List<Document> result = followingRepository.findForumIdsByUserId(userId);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(doc -> doc.containsKey("forumId")));
        assertTrue(result.stream().noneMatch(doc -> doc.containsKey("_id")));
        assertTrue(result.stream().noneMatch(doc -> doc.containsKey("userId")));
    }

    @Test
    void findByUserId_DifferentUsers_IsolatesData() {
        ObjectId userId2 = new ObjectId();
        Following following1 = createFollowing(userId, forumId1);
        Following following2 = createFollowing(userId2, forumId2);
        followingRepository.saveAll(List.of(following1, following2));

        List<Following> user1Results = followingRepository.findByUserId(userId);
        List<Following> user2Results = followingRepository.findByUserId(userId2);

        assertEquals(1, user1Results.size());
        assertEquals(1, user2Results.size());
        assertEquals(forumId1, user1Results.get(0).getForumId());
        assertEquals(forumId2, user2Results.get(0).getForumId());
    }

    @Test
    void findByUserId_WithPageable_EmptyResults() {
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<Following> page = followingRepository.findByUserId(new ObjectId(), pageRequest);

        assertTrue(page.isEmpty());
        assertEquals(0, page.getTotalElements());
        assertFalse(page.hasNext());
    }

    private Following createFollowing(ObjectId userId, ObjectId forumId) {
        return createFollowing(userId, forumId, Instant.now());
    }

    private Following createFollowing(ObjectId userId, ObjectId forumId, Instant createdAt) {
        return Following.builder()
                .userId(userId)
                .forumId(forumId)
                .createdAt(createdAt)
                .build();
    }
}