package org.example.backend.forum;

import org.bson.types.ObjectId;
import org.example.backend.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ForumRepositoryTest extends AbstractMongoIntegrationTest {

    @Autowired
    private ForumRepository forumRepository;

    private ObjectId ownerId;

    @BeforeEach
    void setUp() {
        ownerId = new ObjectId();
    }

    @Test
    void save_ValidForum_Success() {
        Forum forum = createForum("Test Forum", "Test Description");

        Forum saved = forumRepository.save(forum);

        assertNotNull(saved.getId());
        assertEquals("Test Forum", saved.getName());
        assertEquals("Test Description", saved.getDescription());
        assertEquals(ownerId, saved.getOwnerId());
    }

    @Test
    void findById_ExistingForum_ReturnsForum() {
        Forum forum = createForum("Test Forum", "Test Description");
        Forum saved = forumRepository.save(forum);

        Optional<Forum> found = forumRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("Test Forum", found.get().getName());
    }

    @Test
    void findById_NonExistingForum_ReturnsEmpty() {
        Optional<Forum> found = forumRepository.findById(new ObjectId());

        assertFalse(found.isPresent());
    }

    @Test
    void deleteById_ExistingForum_DeletesSuccessfully() {
        Forum forum = createForum("Test Forum", "Test Description");
        Forum saved = forumRepository.save(forum);

        forumRepository.deleteById(saved.getId());

        assertFalse(forumRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void findByOwnerId_ReturnsAllForumsForOwner() {
        Forum forum1 = createForum("Forum 1", "Description 1");
        Forum forum2 = createForum("Forum 2", "Description 2");
        Forum forum3 = createForum("Forum 3", "Description 3");
        forumRepository.saveAll(List.of(forum1, forum2, forum3));

        List<Forum> result = forumRepository.findByOwnerId(ownerId);

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(f -> f.getOwnerId().equals(ownerId)));
    }

    @Test
    void findByOwnerId_DifferentOwner_ReturnsOnlyOwnForums() {
        ObjectId owner1 = new ObjectId();
        ObjectId owner2 = new ObjectId();

        Forum forum1 = createForumWithOwner("Forum 1", "Desc 1", owner1);
        Forum forum2 = createForumWithOwner("Forum 2", "Desc 2", owner2);
        forumRepository.saveAll(List.of(forum1, forum2));

        List<Forum> result = forumRepository.findByOwnerId(owner1);

        assertEquals(1, result.size());
        assertEquals(owner1, result.get(0).getOwnerId());
    }

    @Test
    void findByOwnerId_NoForums_ReturnsEmpty() {
        List<Forum> result = forumRepository.findByOwnerId(new ObjectId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findByNameContainingIgnoreCaseAndIsDeletedFalse_FindsMatches() {
        Forum forum1 = createForum("JavaScript Forum", "JS discussions");
        Forum forum2 = createForum("Java Forum", "Java discussions");
        Forum forum3 = createForum("Python Forum", "Python discussions");
        forumRepository.saveAll(List.of(forum1, forum2, forum3));

        Pageable pageable = PageRequest.of(0, 20);
        Page<Forum> result = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(
                "java", pageable);

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .allMatch(f -> f.getName().toLowerCase().contains("java")));
    }

    @Test
    void findByNameContainingIgnoreCaseAndIsDeletedFalse_CaseInsensitive() {
        Forum forum = createForum("JavaScript Forum", "JS discussions");
        forumRepository.save(forum);

        Pageable pageable = PageRequest.of(0, 20);
        Page<Forum> resultLower = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(
                "javascript", pageable);
        Page<Forum> resultUpper = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(
                "JAVASCRIPT", pageable);
        Page<Forum> resultMixed = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(
                "JaVaScRiPt", pageable);

        assertEquals(1, resultLower.getTotalElements());
        assertEquals(1, resultUpper.getTotalElements());
        assertEquals(1, resultMixed.getTotalElements());
    }

    @Test
    void findByNameContainingIgnoreCaseAndIsDeletedFalse_ExcludesDeletedForums() {
        Forum activeForum = createForum("Active Forum", "Active");
        Forum deletedForum = createForum("Deleted Forum", "Deleted");
        deletedForum.setIsDeleted(true);
        forumRepository.saveAll(List.of(activeForum, deletedForum));

        Pageable pageable = PageRequest.of(0, 20);
        Page<Forum> result = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(
                "Forum", pageable);

        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().get(0).getIsDeleted());
        assertEquals("Active Forum", result.getContent().get(0).getName());
    }

    @Test
    void findByNameContainingIgnoreCaseAndIsDeletedFalse_NoMatches_ReturnsEmpty() {
        Forum forum = createForum("JavaScript Forum", "JS discussions");
        forumRepository.save(forum);

        Pageable pageable = PageRequest.of(0, 20);
        Page<Forum> result = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(
                "nonexistent", pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void findByNameContainingIgnoreCaseAndIsDeletedFalse_Pagination_WorksCorrectly() {
        // Create 25 forums
        for (int i = 0; i < 25; i++) {
            Forum forum = createForum("Test Forum " + i, "Description " + i);
            forumRepository.save(forum);
        }

        Pageable firstPage = PageRequest.of(0, 10);
        Page<Forum> result1 = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(
                "Test", firstPage);

        Pageable secondPage = PageRequest.of(1, 10);
        Page<Forum> result2 = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(
                "Test", secondPage);

        assertEquals(10, result1.getContent().size());
        assertEquals(10, result2.getContent().size());
        assertEquals(25, result1.getTotalElements());
        assertEquals(3, result1.getTotalPages());
        assertTrue(result1.hasNext());
        assertFalse(result1.hasPrevious());
        assertTrue(result2.hasPrevious());
    }

    @Test
    void save_ForumWithAllFields_PreservesAllData() {
        Forum forum = Forum.builder()
                .ownerId(ownerId)
                .name("Complete Forum")
                .description("Full description")
                .followerCount(100)
                .postCount(50)
                .createdAt(Instant.now())
                .isDeleted(false)
                .build();

        Forum saved = forumRepository.save(forum);

        assertEquals("Complete Forum", saved.getName());
        assertEquals("Full description", saved.getDescription());
        assertEquals(100, saved.getFollowerCount());
        assertEquals(50, saved.getPostCount());
        assertNotNull(saved.getCreatedAt());
        assertFalse(saved.getIsDeleted());
        assertNull(saved.getDeletedAt());
    }

    @Test
    void save_ForumWithDefaultValues_UsesDefaults() {
        Forum forum = Forum.builder()
                .ownerId(ownerId)
                .name("Default Forum")
                .description("Default description")
                .build();

        Forum saved = forumRepository.save(forum);

        assertEquals(0, saved.getFollowerCount());
        assertEquals(0, saved.getPostCount());
        assertFalse(saved.getIsDeleted());
        assertNull(saved.getDeletedAt());
    }

    @Test
    void save_SoftDeletedForum_PreservesDeletedState() {
        Forum forum = Forum.builder()
                .ownerId(ownerId)
                .name("Deleted Forum")
                .description("Deleted")
                .isDeleted(true)
                .deletedAt(Instant.now())
                .build();

        Forum saved = forumRepository.save(forum);

        assertTrue(saved.getIsDeleted());
        assertNotNull(saved.getDeletedAt());
    }

    @Test
    void update_ExistingForum_UpdatesSuccessfully() {
        Forum forum = createForum("Original Name", "Original Description");
        Forum saved = forumRepository.save(forum);

        saved.setName("Updated Name");
        saved.setDescription("Updated Description");
        saved.setFollowerCount(10);
        Forum updated = forumRepository.save(saved);

        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated Description", updated.getDescription());
        assertEquals(10, updated.getFollowerCount());
    }

    @Test
    void findAll_ReturnsAllForums() {
        Forum forum1 = createForum("Forum 1", "Desc 1");
        Forum forum2 = createForum("Forum 2", "Desc 2");
        forumRepository.saveAll(List.of(forum1, forum2));

        List<Forum> all = forumRepository.findAll();

        assertTrue(all.size() >= 2);
    }

    @Test
    void count_ReturnsCorrectCount() {
        Forum forum1 = createForum("Forum 1", "Desc 1");
        Forum forum2 = createForum("Forum 2", "Desc 2");
        forumRepository.saveAll(List.of(forum1, forum2));

        long count = forumRepository.count();

        assertTrue(count >= 2);
    }

    @Test
    void findByNameContainingIgnoreCaseAndIsDeletedFalse_PartialMatch() {
        Forum forum = createForum("JavaScript Development Forum", "JS dev");
        forumRepository.save(forum);

        Pageable pageable = PageRequest.of(0, 20);
        Page<Forum> result = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(
                "Development", pageable);

        assertEquals(1, result.getTotalElements());
    }

    private Forum createForum(String name, String description) {
        return createForumWithOwner(name, description, ownerId);
    }

    private Forum createForumWithOwner(String name, String description, ObjectId ownerId) {
        return Forum.builder()
                .ownerId(ownerId)
                .name(name)
                .description(description)
                .createdAt(Instant.now())
                .build();
    }
}