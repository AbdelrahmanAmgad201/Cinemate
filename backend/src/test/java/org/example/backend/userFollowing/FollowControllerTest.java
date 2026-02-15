package org.example.backend.userFollowing;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.userfollowing.FollowController;
import org.example.backend.userfollowing.FollowService;
import org.example.backend.userfollowing.FollowerView;
import org.example.backend.userfollowing.FollowingView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class FollowControllerTest {

    private FollowService followService;
    private FollowController followController;
    private HttpServletRequest request;

    @BeforeEach
    void setup() {
        followService = mock(FollowService.class);
        followController = new FollowController(followService);
        request = mock(HttpServletRequest.class);
    }

    // =============== Follow Tests ===============
    @Test
    void testFollow_Success() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        doNothing().when(followService).follow(followingUserId, followedUserId);

        ResponseEntity<?> response = followController.follow(request, followedUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(request).getAttribute("userId");
        verify(followService).follow(followingUserId, followedUserId);
    }

    @Test
    void testFollow_WithDifferentUserIds() {
        Long followingUserId = 10L;
        Long followedUserId = 20L;

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        doNothing().when(followService).follow(followingUserId, followedUserId);

        ResponseEntity<?> response = followController.follow(request, followedUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(followService).follow(followingUserId, followedUserId);
    }

    @Test
    void testFollow_ExtractsUserIdFromRequest() {
        Long followingUserId = 5L;
        Long followedUserId = 3L;

        when(request.getAttribute("userId")).thenReturn(followingUserId);

        followController.follow(request, followedUserId);

        verify(request).getAttribute("userId");
        verify(followService).follow(followingUserId, followedUserId);
    }

    // =============== Unfollow Tests ===============
    @Test
    void testUnfollow_Success() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        doNothing().when(followService).unfollow(followingUserId, followedUserId);

        ResponseEntity<?> response = followController.unFollow(request, followedUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(request).getAttribute("userId");
        verify(followService).unfollow(followingUserId, followedUserId);
    }

    @Test
    void testUnfollow_WithDifferentUserIds() {
        Long followingUserId = 15L;
        Long followedUserId = 25L;

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        doNothing().when(followService).unfollow(followingUserId, followedUserId);

        ResponseEntity<?> response = followController.unFollow(request, followedUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(followService).unfollow(followingUserId, followedUserId);
    }

    @Test
    void testUnfollow_ExtractsUserIdFromRequest() {
        Long followingUserId = 7L;
        Long followedUserId = 9L;

        when(request.getAttribute("userId")).thenReturn(followingUserId);

        followController.unFollow(request, followedUserId);

        verify(request).getAttribute("userId");
        verify(followService).unfollow(followingUserId, followedUserId);
    }

    // =============== IsFollowed Tests ===============
    @Test
    void testIsFollowed_ReturnsTrue() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        when(followService.isFollowed(followingUserId, followedUserId)).thenReturn(true);

        ResponseEntity<?> response = followController.isFollowed(request, followedUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody());
        verify(request).getAttribute("userId");
        verify(followService).isFollowed(followingUserId, followedUserId);
    }

    @Test
    void testIsFollowed_ReturnsFalse() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        when(followService.isFollowed(followingUserId, followedUserId)).thenReturn(false);

        ResponseEntity<?> response = followController.isFollowed(request, followedUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody());
        verify(followService).isFollowed(followingUserId, followedUserId);
    }

    @Test
    void testIsFollowed_ExtractsUserIdFromRequest() {
        Long followingUserId = 11L;
        Long followedUserId = 12L;

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        when(followService.isFollowed(followingUserId, followedUserId)).thenReturn(false);

        followController.isFollowed(request, followedUserId);

        verify(request).getAttribute("userId");
        verify(followService).isFollowed(followingUserId, followedUserId);
    }

    // =============== GetFollowers Tests ===============
    @Test
    void testGetFollowers_Success() {
        Long followedUserId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        FollowerView follower1 = mock(FollowerView.class);
        FollowerView follower2 = mock(FollowerView.class);
        List<FollowerView> followersList = Arrays.asList(follower1, follower2);
        Page<FollowerView> followersPage = new PageImpl<>(followersList, pageable, 2);

        when(request.getAttribute("userId")).thenReturn(followedUserId);
        when(followService.getUserFollowers(followedUserId, pageable)).thenReturn(followersPage);

        ResponseEntity<Page<FollowerView>> response = followController.getFollowers(request, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        assertEquals(followersPage, response.getBody());
        verify(request).getAttribute("userId");
        verify(followService).getUserFollowers(followedUserId, pageable);
    }

    @Test
    void testGetFollowers_EmptyPage() {
        Long followedUserId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        Page<FollowerView> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        when(request.getAttribute("userId")).thenReturn(followedUserId);
        when(followService.getUserFollowers(followedUserId, pageable)).thenReturn(emptyPage);

        ResponseEntity<Page<FollowerView>> response = followController.getFollowers(request, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getContent().isEmpty());
        assertEquals(0, response.getBody().getTotalElements());
    }

    @Test
    void testGetFollowers_WithCustomPageable() {
        Long followedUserId = 1L;
        Pageable pageable = PageRequest.of(2, 10);

        Page<FollowerView> followersPage = new PageImpl<>(Arrays.asList());

        when(request.getAttribute("userId")).thenReturn(followedUserId);
        when(followService.getUserFollowers(followedUserId, pageable)).thenReturn(followersPage);

        ResponseEntity<Page<FollowerView>> response = followController.getFollowers(request, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(followService).getUserFollowers(followedUserId, pageable);
    }

    @Test
    void testGetFollowers_DefaultPageSize() {
        Long followedUserId = 1L;
        Pageable pageable = PageRequest.of(0, 20); // Default size from @PageableDefault

        Page<FollowerView> followersPage = new PageImpl<>(Arrays.asList());

        when(request.getAttribute("userId")).thenReturn(followedUserId);
        when(followService.getUserFollowers(followedUserId, pageable)).thenReturn(followersPage);

        ResponseEntity<Page<FollowerView>> response = followController.getFollowers(request, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(followService).getUserFollowers(followedUserId, pageable);
    }

    @Test
    void testGetFollowers_ExtractsUserIdFromRequest() {
        Long followedUserId = 5L;
        Pageable pageable = PageRequest.of(0, 20);
        Page<FollowerView> followersPage = new PageImpl<>(Arrays.asList());

        when(request.getAttribute("userId")).thenReturn(followedUserId);
        when(followService.getUserFollowers(followedUserId, pageable)).thenReturn(followersPage);

        followController.getFollowers(request, pageable);

        verify(request).getAttribute("userId");
    }

    // =============== GetFollowings Tests ===============
    @Test
    void testGetFollowings_Success() {
        Long followingUserId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        FollowingView following1 = mock(FollowingView.class);
        FollowingView following2 = mock(FollowingView.class);
        FollowingView following3 = mock(FollowingView.class);
        List<FollowingView> followingsList = Arrays.asList(following1, following2, following3);
        Page<FollowingView> followingsPage = new PageImpl<>(followingsList, pageable, 3);

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        when(followService.getUserFollowings(followingUserId, pageable)).thenReturn(followingsPage);

        ResponseEntity<Page<FollowingView>> response = followController.getFollowings(request, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().getContent().size());
        assertEquals(followingsPage, response.getBody());
        verify(request).getAttribute("userId");
        verify(followService).getUserFollowings(followingUserId, pageable);
    }

    @Test
    void testGetFollowings_EmptyPage() {
        Long followingUserId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        Page<FollowingView> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        when(followService.getUserFollowings(followingUserId, pageable)).thenReturn(emptyPage);

        ResponseEntity<Page<FollowingView>> response = followController.getFollowings(request, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getContent().isEmpty());
        assertEquals(0, response.getBody().getTotalElements());
    }

    @Test
    void testGetFollowings_WithCustomPageable() {
        Long followingUserId = 1L;
        Pageable pageable = PageRequest.of(1, 15);

        Page<FollowingView> followingsPage = new PageImpl<>(Arrays.asList());

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        when(followService.getUserFollowings(followingUserId, pageable)).thenReturn(followingsPage);

        ResponseEntity<Page<FollowingView>> response = followController.getFollowings(request, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(followService).getUserFollowings(followingUserId, pageable);
    }

    @Test
    void testGetFollowings_DefaultPageSize() {
        Long followingUserId = 1L;
        Pageable pageable = PageRequest.of(0, 20); // Default size from @PageableDefault

        Page<FollowingView> followingsPage = new PageImpl<>(Arrays.asList());

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        when(followService.getUserFollowings(followingUserId, pageable)).thenReturn(followingsPage);

        ResponseEntity<Page<FollowingView>> response = followController.getFollowings(request, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(followService).getUserFollowings(followingUserId, pageable);
    }

    @Test
    void testGetFollowings_ExtractsUserIdFromRequest() {
        Long followingUserId = 8L;
        Pageable pageable = PageRequest.of(0, 20);
        Page<FollowingView> followingsPage = new PageImpl<>(Arrays.asList());

        when(request.getAttribute("userId")).thenReturn(followingUserId);
        when(followService.getUserFollowings(followingUserId, pageable)).thenReturn(followingsPage);

        followController.getFollowings(request, pageable);

        verify(request).getAttribute("userId");
    }

    // =============== Integration Tests ===============
    @Test
    void testAllEndpoints_UseCorrectServiceMethods() {
        Long userId = 1L;
        Long targetUserId = 2L;
        Pageable pageable = PageRequest.of(0, 20);

        when(request.getAttribute("userId")).thenReturn(userId);
        when(followService.isFollowed(anyLong(), anyLong())).thenReturn(false);
        when(followService.getUserFollowers(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList()));
        when(followService.getUserFollowings(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList()));

        // Test all endpoints
        followController.follow(request, targetUserId);
        followController.unFollow(request, targetUserId);
        followController.isFollowed(request, targetUserId);
        followController.getFollowers(request, pageable);
        followController.getFollowings(request, pageable);

        // Verify all service methods were called
        verify(followService).follow(userId, targetUserId);
        verify(followService).unfollow(userId, targetUserId);
        verify(followService).isFollowed(userId, targetUserId);
        verify(followService).getUserFollowers(userId, pageable);
        verify(followService).getUserFollowings(userId, pageable);
    }

    @Test
    void testAllEndpoints_ReturnOkStatus() {
        Long userId = 1L;
        Long targetUserId = 2L;
        Pageable pageable = PageRequest.of(0, 20);

        when(request.getAttribute("userId")).thenReturn(userId);
        when(followService.isFollowed(anyLong(), anyLong())).thenReturn(true);
        when(followService.getUserFollowers(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList()));
        when(followService.getUserFollowings(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList()));

        ResponseEntity<?> followResponse = followController.follow(request, targetUserId);
        ResponseEntity<?> unfollowResponse = followController.unFollow(request, targetUserId);
        ResponseEntity<?> isFollowedResponse = followController.isFollowed(request, targetUserId);
        ResponseEntity<Page<FollowerView>> followersResponse = followController.getFollowers(request, pageable);
        ResponseEntity<Page<FollowingView>> followingsResponse = followController.getFollowings(request, pageable);

        assertEquals(HttpStatus.OK, followResponse.getStatusCode());
        assertEquals(HttpStatus.OK, unfollowResponse.getStatusCode());
        assertEquals(HttpStatus.OK, isFollowedResponse.getStatusCode());
        assertEquals(HttpStatus.OK, followersResponse.getStatusCode());
        assertEquals(HttpStatus.OK, followingsResponse.getStatusCode());
    }
}