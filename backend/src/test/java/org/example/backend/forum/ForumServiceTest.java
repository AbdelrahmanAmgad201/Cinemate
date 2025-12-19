package org.example.backend.forum;

import org.bson.types.ObjectId;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.hateSpeach.HateSpeachService;
import org.example.backend.hateSpeach.HateSpeechException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ForumServiceTest {

    @Mock
    private ForumRepository forumRepository;

    @Mock
    private CascadeDeletionService deletionService;

    @Mock
    private AccessService accessService;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private HateSpeachService hateSpeachService;

    @InjectMocks
    private ForumService forumService;

    private Long userId;
    private ObjectId userObjectId;
    private ObjectId forumId;
    private Forum testForum;
    private ForumCreationRequest creationRequest;

    @BeforeEach
    void setUp() {
        userId = 123456L;
        userObjectId = new ObjectId(String.format("%024x", userId));
        forumId = new ObjectId();

        testForum = Forum.builder()
                .id(forumId)
                .ownerId(userObjectId)
                .name("Test Forum")
                .description("Test Description")
                .followerCount(10)
                .postCount(5)
                .createdAt(Instant.now())
                .isDeleted(false)
                .build();

        creationRequest = ForumCreationRequest.builder()
                .name("New Forum")
                .description("New Forum Description")
                .build();
    }

    // ==================== CREATE FORUM TESTS ====================

    @Test
    void createForum_ValidRequest_Success() {
        // FIXED: Both name AND description must return true (no hate speech)
        when(hateSpeachService.analyzeText("New Forum")).thenReturn(true);
        when(hateSpeachService.analyzeText("New Forum Description")).thenReturn(true);
        when(forumRepository.save(any(Forum.class))).thenAnswer(i -> {
            Forum forum = i.getArgument(0);
            forum.setId(forumId);
            return forum;
        });

        Forum result = forumService.createForum(creationRequest, userId);

        assertNotNull(result);
        assertEquals("New Forum", result.getName());
        assertEquals("New Forum Description", result.getDescription());
        assertEquals(userObjectId, result.getOwnerId());
        assertNotNull(result.getCreatedAt());

        verify(hateSpeachService).analyzeText("New Forum");
        verify(hateSpeachService).analyzeText("New Forum Description");
        verify(forumRepository).save(argThat(forum ->
                forum.getName().equals("New Forum") &&
                        forum.getDescription().equals("New Forum Description") &&
                        forum.getOwnerId().equals(userObjectId)
        ));
    }

    @Test
    void createForum_HateSpeechInName_ThrowsHateSpeechException() {
        // FIXED: Name returns false (hate speech detected)
        when(hateSpeachService.analyzeText("New Forum")).thenReturn(false);
        // Description won't be checked due to short-circuit evaluation (|| operator)

        assertThrows(HateSpeechException.class,
                () -> forumService.createForum(creationRequest, userId));

        verify(hateSpeachService).analyzeText("New Forum");
        // FIXED: Description is NOT checked due to short-circuit
        verify(hateSpeachService, never()).analyzeText("New Forum Description");
        verify(forumRepository, never()).save(any());
    }

    @Test
    void createForum_HateSpeechInBothNameAndDescription_ThrowsHateSpeechException() {
        // FIXED: Name returns false - description won't be checked
        when(hateSpeachService.analyzeText("New Forum")).thenReturn(false);

        assertThrows(HateSpeechException.class,
                () -> forumService.createForum(creationRequest, userId));

        verify(hateSpeachService).analyzeText("New Forum");
        // FIXED: Description check is skipped due to short-circuit
        verify(hateSpeachService, never()).analyzeText("New Forum Description");
        verify(forumRepository, never()).save(any());
    }

    @Test
    void createForum_CleanNameDirtyDescription_ThrowsHateSpeechException() {
        // FIXED: Name is clean (true), description is dirty (false)
        // With || logic: !true || !false = false || true = true (throws exception)
        when(hateSpeachService.analyzeText("New Forum")).thenReturn(true);
        when(hateSpeachService.analyzeText("New Forum Description")).thenReturn(false);

        assertThrows(HateSpeechException.class,
                () -> forumService.createForum(creationRequest, userId));

        verify(hateSpeachService).analyzeText("New Forum");
        verify(hateSpeachService).analyzeText("New Forum Description");
        verify(forumRepository, never()).save(any());
    }

    @Test
    void createForum_DefaultValues_AreSet() {
        when(hateSpeachService.analyzeText(anyString())).thenReturn(true);
        when(forumRepository.save(any(Forum.class))).thenAnswer(i -> i.getArgument(0));

        Forum result = forumService.createForum(creationRequest, userId);

        assertEquals(0, result.getFollowerCount());
        assertEquals(0, result.getPostCount());
        assertFalse(result.getIsDeleted());
        assertNull(result.getDeletedAt());
    }

    @Test
    void createForum_SetsCreatedAtTimestamp() {
        when(hateSpeachService.analyzeText(anyString())).thenReturn(true);
        Instant before = Instant.now();
        when(forumRepository.save(any(Forum.class))).thenAnswer(i -> i.getArgument(0));

        Forum result = forumService.createForum(creationRequest, userId);
        Instant after = Instant.now();

        assertNotNull(result.getCreatedAt());
        assertTrue(result.getCreatedAt().isAfter(before) || result.getCreatedAt().equals(before));
        assertTrue(result.getCreatedAt().isBefore(after) || result.getCreatedAt().equals(after));
    }

    // ==================== DELETE FORUM TESTS ====================

    @Test
    void deleteForum_AuthorizedUser_Success() {
        when(accessService.canDeleteForum(userObjectId, forumId)).thenReturn(true);
        doNothing().when(deletionService).deleteForum(forumId);

        forumService.deleteForum(forumId, userId);

        verify(accessService).canDeleteForum(userObjectId, forumId);
        verify(deletionService).deleteForum(forumId);
    }

    @Test
    void deleteForum_UnauthorizedUser_ThrowsAccessDeniedException() {
        when(accessService.canDeleteForum(userObjectId, forumId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> forumService.deleteForum(forumId, userId));

        verify(accessService).canDeleteForum(userObjectId, forumId);
        verify(deletionService, never()).deleteForum(any());
    }

    @Test
    void deleteForum_CallsDeletionService() {
        when(accessService.canDeleteForum(userObjectId, forumId)).thenReturn(true);
        doNothing().when(deletionService).deleteForum(forumId);

        forumService.deleteForum(forumId, userId);

        verify(deletionService).deleteForum(forumId);
    }

    // ==================== UPDATE FORUM TESTS ====================

    @Test
    void updateForum_Owner_Success() {
        ForumCreationRequest updateRequest = ForumCreationRequest.builder()
                .name("Updated Forum")
                .description("Updated Description")
                .build();
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);
        // FIXED: Both must return true
        when(hateSpeachService.analyzeText("Updated Forum")).thenReturn(true);
        when(hateSpeachService.analyzeText("Updated Description")).thenReturn(true);
        when(forumRepository.save(any(Forum.class))).thenAnswer(i -> i.getArgument(0));

        Forum result = forumService.updateForum(forumId, updateRequest, userId);

        assertEquals("Updated Forum", result.getName());
        assertEquals("Updated Description", result.getDescription());
        verify(hateSpeachService).analyzeText("Updated Forum");
        verify(hateSpeachService).analyzeText("Updated Description");
        verify(forumRepository).save(testForum);
    }

    @Test
    void updateForum_HateSpeechInName_ThrowsHateSpeechException() {
        ForumCreationRequest updateRequest = ForumCreationRequest.builder()
                .name("Hateful Forum")
                .description("Updated Description")
                .build();

        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);
        // FIXED: Name returns false - description won't be checked
        when(hateSpeachService.analyzeText("Hateful Forum")).thenReturn(false);

        assertThrows(HateSpeechException.class,
                () -> forumService.updateForum(forumId, updateRequest, userId));

        verify(hateSpeachService).analyzeText("Hateful Forum");
        // FIXED: Description is NOT checked due to short-circuit
        verify(hateSpeachService, never()).analyzeText("Updated Description");
        verify(forumRepository, never()).save(any());
    }

    @Test
    void updateForum_CleanNameDirtyDescription_ThrowsHateSpeechException() {
        ForumCreationRequest updateRequest = ForumCreationRequest.builder()
                        .name("Clean Forum")
                        .description("Hateful Description")
                        .build();

        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);
        // FIXED: Name clean (true), description dirty (false)
        when(hateSpeachService.analyzeText("Clean Forum")).thenReturn(true);
        when(hateSpeachService.analyzeText("Hateful Description")).thenReturn(false);

        assertThrows(HateSpeechException.class,
                () -> forumService.updateForum(forumId, updateRequest, userId));

        verify(hateSpeachService).analyzeText("Clean Forum");
        verify(hateSpeachService).analyzeText("Hateful Description");
        verify(forumRepository, never()).save(any());
    }

    @Test
    void updateForum_NotOwner_ThrowsAccessDeniedException() {
        Long differentUserId = 999999L;
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);

        assertThrows(AccessDeniedException.class,
                () -> forumService.updateForum(forumId, creationRequest, differentUserId));

        verify(hateSpeachService, never()).analyzeText(anyString());
        verify(forumRepository, never()).save(any());
    }

    @Test
    void updateForum_ForumNotFound_ThrowsIllegalArgumentException() {
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> forumService.updateForum(forumId, creationRequest, userId));

        verify(hateSpeachService, never()).analyzeText(anyString());
        verify(forumRepository, never()).save(any());
    }

    @Test
    void updateForum_DeletedForum_ThrowsIllegalStateException() {
        testForum.setIsDeleted(true);
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);

        assertThrows(IllegalStateException.class,
                () -> forumService.updateForum(forumId, creationRequest, userId));

        verify(hateSpeachService, never()).analyzeText(anyString());
        verify(forumRepository, never()).save(any());
    }

    @Test
    void updateForum_PreservesOtherFields() {
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);
        when(hateSpeachService.analyzeText(anyString())).thenReturn(true);
        when(forumRepository.save(any(Forum.class))).thenAnswer(i -> i.getArgument(0));

        Forum result = forumService.updateForum(forumId, creationRequest, userId);

        // Should preserve these fields
        assertEquals(forumId, result.getId());
        assertEquals(userObjectId, result.getOwnerId());
        assertEquals(10, result.getFollowerCount());
        assertEquals(5, result.getPostCount());
        assertFalse(result.getIsDeleted());
    }

    @Test
    void updateForum_OnlyUpdatesNameAndDescription() {
        ForumCreationRequest updateRequest = ForumCreationRequest.builder()
                .name("New Name")
                .description("New Description")
                .build();

        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);
        when(hateSpeachService.analyzeText(anyString())).thenReturn(true);
        when(forumRepository.save(any(Forum.class))).thenAnswer(i -> i.getArgument(0));

        Forum result = forumService.updateForum(forumId, updateRequest, userId);

        assertEquals("New Name", result.getName());
        assertEquals("New Description", result.getDescription());
        // Verify other fields unchanged
        assertEquals(userObjectId, result.getOwnerId());
        assertEquals(10, result.getFollowerCount());
    }

    // ==================== SEARCH FORUMS TESTS ====================

    @Test
    void searchForums_WithResults_ReturnsSearchResultDto() {
        Pageable pageable = PageRequest.of(0, 20);

        Forum forum1 = Forum.builder()
                .id(new ObjectId())
                .name("JavaScript Forum")
                .description("JS discussions")
                .isDeleted(false)
                .build();

        Forum forum2 = Forum.builder()
                .id(new ObjectId())
                .name("Java Forum")
                .description("Java discussions")
                .isDeleted(false)
                .build();

        List<Forum> forums = Arrays.asList(forum1, forum2);
        Page<Forum> forumPage = new PageImpl<>(forums, pageable, 2);

        when(forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse("java", pageable))
                .thenReturn(forumPage);

        SearchResultDto result = forumService.searchForums("java", pageable);

        assertNotNull(result);
        assertEquals(2, result.getForums().size());
        assertEquals(0, result.getCurrentPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getTotalElements());
        assertEquals(20, result.getPageSize());
        assertFalse(result.isHasNext());
        assertFalse(result.isHasPrevious());
    }

    @Test
    void searchForums_NoResults_ReturnsEmptySearchResultDto() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Forum> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse("nonexistent", pageable))
                .thenReturn(emptyPage);

        SearchResultDto result = forumService.searchForums("nonexistent", pageable);

        assertNotNull(result);
        assertTrue(result.getForums().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void searchForums_WithPagination_CorrectFlags() {
        Pageable pageable = PageRequest.of(1, 10);

        List<Forum> forums = Arrays.asList(testForum);
        Page<Forum> forumPage = new PageImpl<>(forums, pageable, 25);

        when(forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse("test", pageable))
                .thenReturn(forumPage);

        SearchResultDto result = forumService.searchForums("test", pageable);

        assertEquals(1, result.getCurrentPage());
        assertEquals(3, result.getTotalPages());
        assertEquals(25, result.getTotalElements());
        assertEquals(10, result.getPageSize());
        assertTrue(result.isHasNext());
        assertTrue(result.isHasPrevious());
    }

    @Test
    void searchForums_LastPage_HasNextFalse() {
        Pageable pageable = PageRequest.of(2, 10);

        List<Forum> forums = Arrays.asList(testForum);
        Page<Forum> forumPage = new PageImpl<>(forums, pageable, 25);

        when(forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse("test", pageable))
                .thenReturn(forumPage);

        SearchResultDto result = forumService.searchForums("test", pageable);

        assertFalse(result.isHasNext());
        assertTrue(result.isHasPrevious());
    }

    @Test
    void searchForums_CaseInsensitive() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Forum> forumPage = new PageImpl<>(List.of(testForum), pageable, 1);

        when(forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse("TEST", pageable))
                .thenReturn(forumPage);

        SearchResultDto result = forumService.searchForums("TEST", pageable);

        assertNotNull(result);
        assertEquals(1, result.getForums().size());
        verify(forumRepository).findByNameContainingIgnoreCaseAndIsDeletedFalse("TEST", pageable);
    }

    // ==================== GET FORUM BY ID TESTS ====================

    @Test
    void getForumById_ValidNonDeletedForum_Success() {
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);

        Forum result = forumService.getForumById(forumId);

        assertNotNull(result);
        assertEquals(forumId, result.getId());
        assertEquals("Test Forum", result.getName());
        assertFalse(result.getIsDeleted());
        verify(mongoTemplate).findById(forumId, Forum.class);
    }

    @Test
    void getForumById_DeletedForum_ThrowsIllegalStateException() {
        testForum.setIsDeleted(true);
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);

        assertThrows(IllegalStateException.class,
                () -> forumService.getForumById(forumId));

        verify(mongoTemplate).findById(forumId, Forum.class);
    }

    @Test
    void getForumById_ForumNotFound_ThrowsNullPointerException() {
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> forumService.getForumById(forumId));

        verify(mongoTemplate).findById(forumId, Forum.class);
    }
}