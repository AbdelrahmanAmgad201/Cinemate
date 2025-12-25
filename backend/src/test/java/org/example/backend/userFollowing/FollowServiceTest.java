package org.example.backend.userFollowing;

import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.example.backend.userfollowing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class FollowServiceTest {

    private UserRepository userRepository;
    private FollowsRepository followsRepository;
    private FollowService followService;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        followsRepository = mock(FollowsRepository.class);
        followService = new FollowService(userRepository, followsRepository);
    }

    // =============== Helper Methods ===============
    private User createUser(Long id, int followers, int following) {
        User user = new User();
        user.setId(id);
        user.setNumberOfFollowers(followers);
        user.setNumberOfFollowing(following);
        return user;
    }

    private Follows createFollows(Long followingId, Long followedId, boolean isDeleted) {
        User followingUser = createUser(followingId, 0, 0);
        User followedUser = createUser(followedId, 0, 0);

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingId)
                .followedUserId(followedId)
                .build();

        return Follows.builder()
                .followsID(followsId)
                .followingUser(followingUser)
                .followedUser(followedUser)
                .isDeleted(isDeleted)
                .followedAt(LocalDateTime.now())
                .build();
    }

    // =============== Follow Tests ===============
    @Test
    void testFollow_NewFollow_Success() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        User followingUser = createUser(followingUserId, 0, 5);
        User followedUser = createUser(followedUserId, 10, 0);

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.empty());
        when(userRepository.findById(followingUserId)).thenReturn(Optional.of(followingUser));
        when(userRepository.findById(followedUserId)).thenReturn(Optional.of(followedUser));
        when(userRepository.save(any(User.class))).thenReturn(new User());
        when(followsRepository.save(any(Follows.class))).thenReturn(new Follows());

        followService.follow(followingUserId, followedUserId);

        // Verify follower counts updated
        assertEquals(6, followingUser.getNumberOfFollowing());
        assertEquals(11, followedUser.getNumberOfFollowers());

        // Verify saves were called
        verify(userRepository, times(2)).save(any(User.class));
        verify(followsRepository).save(any(Follows.class));

        // Verify Follows object was created correctly
        ArgumentCaptor<Follows> followsCaptor = ArgumentCaptor.forClass(Follows.class);
        verify(followsRepository).save(followsCaptor.capture());
        Follows savedFollows = followsCaptor.getValue();
        assertEquals(followingUser, savedFollows.getFollowingUser());
        assertEquals(followedUser, savedFollows.getFollowedUser());
    }

    @Test
    void testFollow_SameUser_NoAction() {
        Long userId = 1L;

        followService.follow(userId, userId);

        // Verify no repository interactions
        verifyNoInteractions(followsRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testFollow_AlreadyFollowing_NoAction() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        Follows existingFollow = createFollows(followingUserId, followedUserId, false);

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.of(existingFollow));

        followService.follow(followingUserId, followedUserId);

        // Verify refollow was called but didn't change anything (isDeleted is already false)
        verify(followsRepository).findById(followsId);
        verify(followsRepository, never()).save(any(Follows.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testFollow_Refollow_AfterUnfollow() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        User followingUser = createUser(followingUserId, 0, 5);
        User followedUser = createUser(followedUserId, 10, 0);

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        Follows deletedFollow = Follows.builder()
                .followsID(followsId)
                .followingUser(followingUser)
                .followedUser(followedUser)
                .isDeleted(true)
                .followedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.of(deletedFollow));
        when(userRepository.save(any(User.class))).thenReturn(new User());
        when(followsRepository.save(any(Follows.class))).thenReturn(deletedFollow);

        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);
        followService.follow(followingUserId, followedUserId);
        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);

        // Verify isDeleted was set to false
        assertFalse(deletedFollow.getIsDeleted());

        // Verify followedAt was updated
        assertNotNull(deletedFollow.getFollowedAt());
        assertTrue(deletedFollow.getFollowedAt().isAfter(beforeCall));
        assertTrue(deletedFollow.getFollowedAt().isBefore(afterCall));

        // Verify follower counts updated
        assertEquals(6, followingUser.getNumberOfFollowing());
        assertEquals(11, followedUser.getNumberOfFollowers());

        // Verify saves were called
        verify(userRepository, times(2)).save(any(User.class));
        verify(followsRepository).save(deletedFollow);
    }

    @Test
    void testFollow_FollowingUserNotFound() {
        Long followingUserId = 999L;
        Long followedUserId = 2L;

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.empty());
        when(userRepository.findById(followingUserId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> followService.follow(followingUserId, followedUserId));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testFollow_FollowedUserNotFound() {
        Long followingUserId = 1L;
        Long followedUserId = 999L;

        User followingUser = createUser(followingUserId, 0, 5);

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.empty());
        when(userRepository.findById(followingUserId)).thenReturn(Optional.of(followingUser));
        when(userRepository.findById(followedUserId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> followService.follow(followingUserId, followedUserId));

        assertEquals("User not found", exception.getMessage());
    }

    // =============== Unfollow Tests ===============
    @Test
    void testUnfollow_Success() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        User followingUser = createUser(followingUserId, 0, 10);
        User followedUser = createUser(followedUserId, 20, 0);

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        Follows follows = Follows.builder()
                .followsID(followsId)
                .followingUser(followingUser)
                .followedUser(followedUser)
                .isDeleted(false)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.of(follows));
        when(userRepository.findById(followingUserId)).thenReturn(Optional.of(followingUser));
        when(userRepository.findById(followedUserId)).thenReturn(Optional.of(followedUser));
        when(userRepository.save(any(User.class))).thenReturn(new User());
        when(followsRepository.save(any(Follows.class))).thenReturn(follows);

        followService.unfollow(followingUserId, followedUserId);

        // Verify isDeleted was set to true
        assertTrue(follows.getIsDeleted());

        // Verify follower counts decreased
        assertEquals(9, followingUser.getNumberOfFollowing());
        assertEquals(19, followedUser.getNumberOfFollowers());

        // Verify saves were called
        verify(userRepository, times(2)).save(any(User.class));
        verify(followsRepository).save(follows);
    }

    @Test
    void testUnfollow_NotFollowing_NoAction() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.empty());

        followService.unfollow(followingUserId, followedUserId);

        // Verify no saves were called
        verify(userRepository, never()).save(any(User.class));
        verify(followsRepository, never()).save(any(Follows.class));
    }

    @Test
    void testUnfollow_AlreadyUnfollowed_NoAction() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        User followingUser = createUser(followingUserId, 0, 10);
        User followedUser = createUser(followedUserId, 20, 0);

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        Follows follows = Follows.builder()
                .followsID(followsId)
                .followingUser(followingUser)
                .followedUser(followedUser)
                .isDeleted(true)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.of(follows));

        followService.unfollow(followingUserId, followedUserId);

        // Verify no changes were made
        assertTrue(follows.getIsDeleted());
        assertEquals(10, followingUser.getNumberOfFollowing());
        assertEquals(20, followedUser.getNumberOfFollowers());

        // Verify no saves were called
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any(User.class));
        verify(followsRepository, never()).save(any(Follows.class));
    }

    @Test
    void testUnfollow_FollowingUserNotFound() {
        Long followingUserId = 999L;
        Long followedUserId = 2L;

        User followedUser = createUser(followedUserId, 20, 0);

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        Follows follows = Follows.builder()
                .followsID(followsId)
                .followingUser(new User())
                .followedUser(followedUser)
                .isDeleted(false)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.of(follows));
        when(userRepository.findById(followingUserId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> followService.unfollow(followingUserId, followedUserId));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testUnfollow_FollowedUserNotFound() {
        Long followingUserId = 1L;
        Long followedUserId = 999L;

        User followingUser = createUser(followingUserId, 0, 10);

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        Follows follows = Follows.builder()
                .followsID(followsId)
                .followingUser(followingUser)
                .followedUser(new User())
                .isDeleted(false)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.of(follows));
        when(userRepository.findById(followingUserId)).thenReturn(Optional.of(followingUser));
        when(userRepository.findById(followedUserId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> followService.unfollow(followingUserId, followedUserId));

        assertEquals("User not found", exception.getMessage());
    }

    // =============== IsFollowed Tests ===============
    @Test
    void testIsFollowed_ReturnsTrue() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        Follows follows = createFollows(followingUserId, followedUserId, false);

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.of(follows));

        Boolean result = followService.isFollowed(followingUserId, followedUserId);

        assertTrue(result);
        verify(followsRepository).findById(followsId);
    }

    @Test
    void testIsFollowed_ReturnsFalse_WhenDeleted() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        Follows follows = createFollows(followingUserId, followedUserId, true);

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.of(follows));

        Boolean result = followService.isFollowed(followingUserId, followedUserId);

        assertFalse(result);
        verify(followsRepository).findById(followsId);
    }

    @Test
    void testIsFollowed_ReturnsFalse_WhenNotExists() {
        Long followingUserId = 1L;
        Long followedUserId = 2L;

        FollowsID followsId = FollowsID.builder()
                .followingUserId(followingUserId)
                .followedUserId(followedUserId)
                .build();

        when(followsRepository.findById(followsId)).thenReturn(Optional.empty());

        Boolean result = followService.isFollowed(followingUserId, followedUserId);

        assertFalse(result);
        verify(followsRepository).findById(followsId);
    }

    // =============== GetUserFollowers Tests ===============
    @Test
    void testGetUserFollowers_ReturnsPage() {
        Long followedUserId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        FollowerView follower1 = mock(FollowerView.class);
        FollowerView follower2 = mock(FollowerView.class);
        Page<FollowerView> expectedPage = new PageImpl<>(Arrays.asList(follower1, follower2));

        when(followsRepository.findAllByFollowedUser_IdAndIsDeletedFalse(followedUserId, pageable))
                .thenReturn(expectedPage);

        Page<FollowerView> result = followService.getUserFollowers(followedUserId, pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(expectedPage, result);
        verify(followsRepository).findAllByFollowedUser_IdAndIsDeletedFalse(followedUserId, pageable);
    }

    @Test
    void testGetUserFollowers_EmptyPage() {
        Long followedUserId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<FollowerView> emptyPage = new PageImpl<>(Arrays.asList());

        when(followsRepository.findAllByFollowedUser_IdAndIsDeletedFalse(followedUserId, pageable))
                .thenReturn(emptyPage);

        Page<FollowerView> result = followService.getUserFollowers(followedUserId, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(followsRepository).findAllByFollowedUser_IdAndIsDeletedFalse(followedUserId, pageable);
    }

    @Test
    void testGetUserFollowers_WithPagination() {
        Long followedUserId = 1L;
        Pageable pageable = PageRequest.of(2, 5);

        Page<FollowerView> expectedPage = new PageImpl<>(Arrays.asList());

        when(followsRepository.findAllByFollowedUser_IdAndIsDeletedFalse(followedUserId, pageable))
                .thenReturn(expectedPage);

        Page<FollowerView> result = followService.getUserFollowers(followedUserId, pageable);

        assertNotNull(result);
        verify(followsRepository).findAllByFollowedUser_IdAndIsDeletedFalse(followedUserId, pageable);
    }

    // =============== GetUserFollowings Tests ===============
    @Test
    void testGetUserFollowings_ReturnsPage() {
        Long followingUserId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        FollowingView following1 = mock(FollowingView.class);
        FollowingView following2 = mock(FollowingView.class);
        FollowingView following3 = mock(FollowingView.class);
        Page<FollowingView> expectedPage = new PageImpl<>(Arrays.asList(following1, following2, following3));

        when(followsRepository.findAllByFollowingUser_IdAndIsDeletedFalse(followingUserId, pageable))
                .thenReturn(expectedPage);

        Page<FollowingView> result = followService.getUserFollowings(followingUserId, pageable);

        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(expectedPage, result);
        verify(followsRepository).findAllByFollowingUser_IdAndIsDeletedFalse(followingUserId, pageable);
    }

    @Test
    void testGetUserFollowings_EmptyPage() {
        Long followingUserId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<FollowingView> emptyPage = new PageImpl<>(Arrays.asList());

        when(followsRepository.findAllByFollowingUser_IdAndIsDeletedFalse(followingUserId, pageable))
                .thenReturn(emptyPage);

        Page<FollowingView> result = followService.getUserFollowings(followingUserId, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(followsRepository).findAllByFollowingUser_IdAndIsDeletedFalse(followingUserId, pageable);
    }

    @Test
    void testGetUserFollowings_WithPagination() {
        Long followingUserId = 1L;
        Pageable pageable = PageRequest.of(1, 20);

        Page<FollowingView> expectedPage = new PageImpl<>(Arrays.asList());

        when(followsRepository.findAllByFollowingUser_IdAndIsDeletedFalse(followingUserId, pageable))
                .thenReturn(expectedPage);

        Page<FollowingView> result = followService.getUserFollowings(followingUserId, pageable);

        assertNotNull(result);
        verify(followsRepository).findAllByFollowingUser_IdAndIsDeletedFalse(followingUserId, pageable);
    }
}