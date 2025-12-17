package org.example.backend.forumfollowing;

import org.bson.types.ObjectId;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class FollowingServiceTest {

    @Mock
    private FollowingRepository followingRepository;

    @Mock
    private ForumRepository forumRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private FollowingService followingService;

    private ObjectId forumId;
    private ObjectId userId;
    private Long userIdLong;
    private Forum testForum;
    private Following testFollowing;

    @BeforeEach
    void setUp() {
        forumId = new ObjectId();
        userIdLong = 123456L;
        userId = new ObjectId(String.format("%024x", userIdLong));

        testForum = new Forum();
        testForum.setId(forumId);
        testForum.setFollowerCount(10);
        testForum.setIsDeleted(false);

        testFollowing = Following.builder()
                .id(new ObjectId())
                .userId(userId)
                .forumId(forumId)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void follow_NewFollowing_Success() {
        when(followingRepository.existsByUserIdAndForumId(userId, forumId)).thenReturn(false);
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(testForum));
        when(followingRepository.save(any(Following.class))).thenReturn(testFollowing);

        followingService.follow(forumId, userIdLong);

        verify(followingRepository).save(argThat(following ->
                following.getUserId().equals(userId) &&
                        following.getForumId().equals(forumId) &&
                        following.getCreatedAt() != null
        ));
        verify(forumRepository).save(argThat(forum ->
                forum.getFollowerCount() == 11
        ));
    }

    @Test
    void follow_AlreadyFollowing_DoesNothing() {
        when(followingRepository.existsByUserIdAndForumId(userId, forumId)).thenReturn(true);

        followingService.follow(forumId, userIdLong);

        verify(followingRepository, never()).save(any());
        verify(forumRepository, never()).save(any());
    }

    @Test
    void follow_ForumNotFound_ThrowsException() {
        when(followingRepository.existsByUserIdAndForumId(userId, forumId)).thenReturn(false);
        when(forumRepository.findById(forumId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> followingService.follow(forumId, userIdLong));

        verify(followingRepository, never()).save(any());
    }

    @Test
    void follow_DeletedForum_ThrowsException() {
        testForum.setIsDeleted(true);
        when(followingRepository.existsByUserIdAndForumId(userId, forumId)).thenReturn(false);
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(testForum));

        assertThrows(IllegalStateException.class,
                () -> followingService.follow(forumId, userIdLong));

        verify(followingRepository, never()).save(any());
    }

    @Test
    void unfollow_ExistingFollowing_Success() {
        when(followingRepository.existsByUserIdAndForumId(userId, forumId)).thenReturn(true);
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(testForum));

        followingService.unfollow(forumId, userIdLong);

        verify(followingRepository).deleteByUserIdAndForumId(userId, forumId);
        verify(forumRepository).save(argThat(forum ->
                forum.getFollowerCount() == 9
        ));
    }

    @Test
    void unfollow_NotFollowing_DoesNothing() {
        when(followingRepository.existsByUserIdAndForumId(userId, forumId)).thenReturn(false);

        followingService.unfollow(forumId, userIdLong);

        verify(followingRepository, never()).deleteByUserIdAndForumId(any(), any());
        verify(forumRepository, never()).save(any());
    }

    @Test
    void unfollow_ForumNotFound_DoesNotThrow() {
        when(followingRepository.existsByUserIdAndForumId(userId, forumId)).thenReturn(true);
        when(forumRepository.findById(forumId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> followingService.unfollow(forumId, userIdLong));

        verify(followingRepository).deleteByUserIdAndForumId(userId, forumId);
        verify(forumRepository, never()).save(any());
    }

    @Test
    void unfollow_FollowerCountZero_StaysZero() {
        testForum.setFollowerCount(0);
        when(followingRepository.existsByUserIdAndForumId(userId, forumId)).thenReturn(true);
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(testForum));

        followingService.unfollow(forumId, userIdLong);

        verify(forumRepository).save(argThat(forum ->
                forum.getFollowerCount() == 0
        ));
    }

    @Test
    void getFollowedForums_WithResults_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        ObjectId forumId2 = new ObjectId();

        Following following2 = Following.builder()
                .id(new ObjectId())
                .userId(userId)
                .forumId(forumId2)
                .createdAt(Instant.now())
                .build();

        List<Following> followings = Arrays.asList(testFollowing, following2);
        Page<Following> followingPage = new PageImpl<>(followings, pageable, 2);

        Forum forum2 = new Forum();
        forum2.setId(forumId2);
        forum2.setIsDeleted(false);

        when(followingRepository.findByUserId(userId, pageable)).thenReturn(followingPage);
        when(mongoTemplate.find(any(Query.class), eq(Forum.class)))
                .thenReturn(Arrays.asList(testForum, forum2));

        ForumPageResponse response = followingService.getFollowedForums(userIdLong, pageable);

        assertNotNull(response);
        assertEquals(2, response.getForums().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotalElements());
        assertEquals(20, response.getPageSize());
        assertFalse(response.isHasNext());
        assertFalse(response.isHasPrevious());
    }

    @Test
    void getFollowedForums_EmptyResults_ReturnsEmpty() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Following> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(followingRepository.findByUserId(userId, pageable)).thenReturn(emptyPage);

        ForumPageResponse response = followingService.getFollowedForums(userIdLong, pageable);

        assertNotNull(response);
        assertTrue(response.getForums().isEmpty());
        assertEquals(0, response.getTotalElements());
        verify(mongoTemplate, never()).find(any(), any());
    }

    @Test
    void getFollowedForums_WithPagination_CorrectFlags() {
        Pageable pageable = PageRequest.of(1, 10);
        List<Following> followings = Arrays.asList(testFollowing);
        Page<Following> followingPage = new PageImpl<>(followings, pageable, 25);

        when(followingRepository.findByUserId(userId, pageable)).thenReturn(followingPage);
        when(mongoTemplate.find(any(Query.class), eq(Forum.class)))
                .thenReturn(Arrays.asList(testForum));

        ForumPageResponse response = followingService.getFollowedForums(userIdLong, pageable);

        assertEquals(1, response.getCurrentPage());
        assertEquals(3, response.getTotalPages());
        assertEquals(25, response.getTotalElements());
        assertTrue(response.isHasNext());
        assertTrue(response.isHasPrevious());
    }

    @Test
    void getFollowedForums_FiltersDeletedForums() {
        Pageable pageable = PageRequest.of(0, 20);
        List<Following> followings = Arrays.asList(testFollowing);
        Page<Following> followingPage = new PageImpl<>(followings, pageable, 1);

        when(followingRepository.findByUserId(userId, pageable)).thenReturn(followingPage);
        when(mongoTemplate.find(any(Query.class), eq(Forum.class)))
                .thenReturn(List.of()); // Simulates deleted forum being filtered out

        ForumPageResponse response = followingService.getFollowedForums(userIdLong, pageable);

        assertTrue(response.getForums().isEmpty());
        verify(mongoTemplate).find(argThat(query ->
                query.toString().contains("isDeleted") &&
                        query.toString().contains("false")
        ), eq(Forum.class));
    }
}