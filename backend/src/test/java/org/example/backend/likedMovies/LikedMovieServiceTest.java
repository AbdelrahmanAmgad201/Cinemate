package org.example.backend.likedMovies;

import org.example.backend.likedMovie.*;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.PrivateProfileException;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class LikedMovieServiceTest {

    private LikedMovieRepository likedMovieRepository;
    private MovieRepository movieRepository;
    private UserRepository userRepository;
    private LikedMovieService likedMovieService;

    @BeforeEach
    void setup() {
        likedMovieRepository = mock(LikedMovieRepository.class);
        movieRepository = mock(MovieRepository.class);
        userRepository = mock(UserRepository.class);
        likedMovieService = new LikedMovieService(likedMovieRepository, movieRepository, userRepository);
    }

    // =============== Helper Methods ===============
    private User createUser(Long id, boolean isPublic) {
        User user = new User();
        user.setId(id);
        user.setIsPublic(isPublic);
        return user;
    }

    private Movie createMovie(Long id, String name) {
        Movie movie = new Movie();
        movie.setMovieID(id);
        movie.setName(name);
        return movie;
    }

    private LikedMovie createLikedMovie(Long userId, Long movieId, String movieName, boolean isDeleted) {
        User user = createUser(userId, true);
        Movie movie = createMovie(movieId, movieName);
        LikedMoviesID id = new LikedMoviesID(userId, movieId);

        return LikedMovie.builder()
                .likedMoviesID(id)
                .user(user)
                .movie(movie)
                .movieName(movieName)
                .isDeleted(isDeleted)
                .build();
    }

    // =============== LikeMovie Tests ===============
    @Test
    void testLikeMovie_NewLike_Success() {
        Long userId = 1L;
        Long movieId = 100L;
        String movieName = "Inception";

        User user = createUser(userId, true);
        Movie movie = createMovie(movieId, movieName);
        LikedMoviesID id = new LikedMoviesID(userId, movieId);

        LikedMovie savedLikedMovie = createLikedMovie(userId, movieId, movieName, false);

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(likedMovieRepository.findById(id)).thenReturn(Optional.empty());
        when(likedMovieRepository.save(any(LikedMovie.class))).thenReturn(savedLikedMovie);

        LikedMovie result = likedMovieService.likeMovie(userId, movieId);

        assertNotNull(result);
        assertEquals(movieName, result.getMovieName());
        assertFalse(result.getIsDeleted());

        // Verify the saved object
        ArgumentCaptor<LikedMovie> captor = ArgumentCaptor.forClass(LikedMovie.class);
        verify(likedMovieRepository).save(captor.capture());
        LikedMovie capturedLike = captor.getValue();
        assertEquals(userId, capturedLike.getLikedMoviesID().getUserId());
        assertEquals(movieId, capturedLike.getLikedMoviesID().getMovieId());
        assertEquals(movieName, capturedLike.getMovieName());
    }

    @Test
    void testLikeMovie_AlreadyLiked_ReturnsExisting() {
        Long userId = 1L;
        Long movieId = 100L;

        User user = createUser(userId, true);
        Movie movie = createMovie(movieId, "Inception");
        LikedMoviesID id = new LikedMoviesID(userId, movieId);

        LikedMovie existingLike = createLikedMovie(userId, movieId, "Inception", false);

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(likedMovieRepository.findById(id)).thenReturn(Optional.of(existingLike));

        LikedMovie result = likedMovieService.likeMovie(userId, movieId);

        assertNotNull(result);
        assertSame(existingLike, result);
        assertFalse(result.getIsDeleted());

        // Verify save was NOT called since it's already liked
        verify(likedMovieRepository, never()).save(any(LikedMovie.class));
    }

    @Test
    void testLikeMovie_PreviouslyUnliked_RestoresLike() {
        Long userId = 1L;
        Long movieId = 100L;

        User user = createUser(userId, true);
        Movie movie = createMovie(movieId, "Inception");
        LikedMoviesID id = new LikedMoviesID(userId, movieId);

        LikedMovie deletedLike = createLikedMovie(userId, movieId, "Inception", true);

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(likedMovieRepository.findById(id)).thenReturn(Optional.of(deletedLike));
        when(likedMovieRepository.save(any(LikedMovie.class))).thenReturn(deletedLike);

        LikedMovie result = likedMovieService.likeMovie(userId, movieId);

        assertNotNull(result);
        assertFalse(result.getIsDeleted());

        // Verify isDeleted was set to false and saved
        verify(likedMovieRepository).save(deletedLike);
    }

    @Test
    void testLikeMovie_MovieNotFound() {
        Long userId = 1L;
        Long movieId = 999L;

        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> likedMovieService.likeMovie(userId, movieId));

        assertEquals("Movie not found", exception.getMessage());
        verify(movieRepository).findById(movieId);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(likedMovieRepository);
    }

    @Test
    void testLikeMovie_UserNotFound() {
        Long userId = 999L;
        Long movieId = 100L;

        Movie movie = createMovie(movieId, "Inception");

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> likedMovieService.likeMovie(userId, movieId));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verifyNoInteractions(likedMovieRepository);
    }

    @Test
    void testLikeMovie_MovieNameIsSaved() {
        Long userId = 1L;
        Long movieId = 100L;
        String movieName = "The Matrix";

        User user = createUser(userId, true);
        Movie movie = createMovie(movieId, movieName);
        LikedMoviesID id = new LikedMoviesID(userId, movieId);

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(likedMovieRepository.findById(id)).thenReturn(Optional.empty());
        when(likedMovieRepository.save(any(LikedMovie.class))).thenReturn(new LikedMovie());

        likedMovieService.likeMovie(userId, movieId);

        ArgumentCaptor<LikedMovie> captor = ArgumentCaptor.forClass(LikedMovie.class);
        verify(likedMovieRepository).save(captor.capture());
        assertEquals(movieName, captor.getValue().getMovieName());
    }

    // =============== UnlikeMovie Tests ===============
    @Test
    void testUnlikeMovie_Success() {
        Long userId = 1L;
        Long movieId = 100L;

        LikedMoviesID id = new LikedMoviesID(userId, movieId);
        LikedMovie existingLike = createLikedMovie(userId, movieId, "Inception", false);

        when(likedMovieRepository.findById(id)).thenReturn(Optional.of(existingLike));
        when(likedMovieRepository.save(any(LikedMovie.class))).thenReturn(existingLike);

        likedMovieService.unlikeMovie(userId, movieId);

        assertTrue(existingLike.getIsDeleted());
        verify(likedMovieRepository).save(existingLike);
    }

    @Test
    void testUnlikeMovie_NotLiked_NoAction() {
        Long userId = 1L;
        Long movieId = 100L;

        LikedMoviesID id = new LikedMoviesID(userId, movieId);

        when(likedMovieRepository.findById(id)).thenReturn(Optional.empty());

        likedMovieService.unlikeMovie(userId, movieId);

        verify(likedMovieRepository).findById(id);
        verify(likedMovieRepository, never()).save(any(LikedMovie.class));
    }

    @Test
    void testUnlikeMovie_AlreadyUnliked_NoAction() {
        Long userId = 1L;
        Long movieId = 100L;

        LikedMoviesID id = new LikedMoviesID(userId, movieId);
        LikedMovie deletedLike = createLikedMovie(userId, movieId, "Inception", true);

        when(likedMovieRepository.findById(id)).thenReturn(Optional.of(deletedLike));

        likedMovieService.unlikeMovie(userId, movieId);

        assertTrue(deletedLike.getIsDeleted());
        verify(likedMovieRepository, never()).save(any(LikedMovie.class));
    }

    @Test
    void testUnlikeMovie_SoftDeleteOnly() {
        Long userId = 1L;
        Long movieId = 100L;

        LikedMoviesID id = new LikedMoviesID(userId, movieId);
        LikedMovie existingLike = createLikedMovie(userId, movieId, "Inception", false);

        when(likedMovieRepository.findById(id)).thenReturn(Optional.of(existingLike));
        when(likedMovieRepository.save(any(LikedMovie.class))).thenReturn(existingLike);

        likedMovieService.unlikeMovie(userId, movieId);

        // Verify it's soft deleted, not hard deleted
        verify(likedMovieRepository).save(existingLike);
        verify(likedMovieRepository, never()).delete(any());
        verify(likedMovieRepository, never()).deleteById(any());
    }

    // =============== GetMyLikedMovies Tests ===============
    @Test
    void testGetMyLikedMovies_ReturnsPage() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        LikedMovieView movie1 = mock(LikedMovieView.class);
        LikedMovieView movie2 = mock(LikedMovieView.class);
        Page<LikedMovieView> expectedPage = new PageImpl<>(Arrays.asList(movie1, movie2));

        when(likedMovieRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable))
                .thenReturn(expectedPage);

        Page<LikedMovieView> result = likedMovieService.getMyLikedMovies(userId, pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(expectedPage, result);
        verify(likedMovieRepository).findAllByUserIdAndIsDeletedFalse(userId, pageable);
    }

    @Test
    void testGetMyLikedMovies_EmptyPage() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        Page<LikedMovieView> emptyPage = new PageImpl<>(Arrays.asList());

        when(likedMovieRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable))
                .thenReturn(emptyPage);

        Page<LikedMovieView> result = likedMovieService.getMyLikedMovies(userId, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(likedMovieRepository).findAllByUserIdAndIsDeletedFalse(userId, pageable);
    }

    @Test
    void testGetMyLikedMovies_WithPagination() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(2, 10);

        Page<LikedMovieView> expectedPage = new PageImpl<>(Arrays.asList());

        when(likedMovieRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable))
                .thenReturn(expectedPage);

        Page<LikedMovieView> result = likedMovieService.getMyLikedMovies(userId, pageable);

        assertNotNull(result);
        verify(likedMovieRepository).findAllByUserIdAndIsDeletedFalse(userId, pageable);
    }

    // =============== GetOtherUserLikedMovies Tests ===============
    @Test
    void testGetOtherUserLikedMovies_PublicProfile_Success() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        User publicUser = createUser(userId, true);

        LikedMovieView movie1 = mock(LikedMovieView.class);
        Page<LikedMovieView> expectedPage = new PageImpl<>(Arrays.asList(movie1));

        when(userRepository.findById(userId)).thenReturn(Optional.of(publicUser));
        when(likedMovieRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable))
                .thenReturn(expectedPage);

        Page<LikedMovieView> result = likedMovieService.getOtherUserLikedMovies(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(userRepository).findById(userId);
        verify(likedMovieRepository).findAllByUserIdAndIsDeletedFalse(userId, pageable);
    }

    @Test
    void testGetOtherUserLikedMovies_PrivateProfile_ThrowsException() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        User privateUser = createUser(userId, false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(privateUser));

        PrivateProfileException exception = assertThrows(PrivateProfileException.class,
                () -> likedMovieService.getOtherUserLikedMovies(userId, pageable));

        assertEquals("this profile is private", exception.getMessage());
        verify(userRepository).findById(userId);
        verifyNoInteractions(likedMovieRepository);
    }

    @Test
    void testGetOtherUserLikedMovies_UserNotFound() {
        Long userId = 999L;
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> likedMovieService.getOtherUserLikedMovies(userId, pageable));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verifyNoInteractions(likedMovieRepository);
    }

    @Test
    void testGetOtherUserLikedMovies_PublicProfile_EmptyPage() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        User publicUser = createUser(userId, true);
        Page<LikedMovieView> emptyPage = new PageImpl<>(Arrays.asList());

        when(userRepository.findById(userId)).thenReturn(Optional.of(publicUser));
        when(likedMovieRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable))
                .thenReturn(emptyPage);

        Page<LikedMovieView> result = likedMovieService.getOtherUserLikedMovies(userId, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void testGetOtherUserLikedMovies_ChecksIsPublicFlag() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        User privateUser = createUser(userId, false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(privateUser));

        assertThrows(PrivateProfileException.class,
                () -> likedMovieService.getOtherUserLikedMovies(userId, pageable));

        // Verify the user was fetched and checked
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(userRepository).findById(userIdCaptor.capture());
        assertEquals(userId, userIdCaptor.getValue());
    }

    // =============== IsLiked Tests ===============
    @Test
    void testIsLiked_ReturnsTrue() {
        Long userId = 1L;
        Long movieId = 100L;

        LikedMoviesID id = new LikedMoviesID(userId, movieId);
        LikedMovie likedMovie = createLikedMovie(userId, movieId, "Inception", false);

        when(likedMovieRepository.findById(id)).thenReturn(Optional.of(likedMovie));

        Boolean result = likedMovieService.isLiked(userId, movieId);

        assertTrue(result);
        verify(likedMovieRepository).findById(id);
    }

    @Test
    void testIsLiked_ReturnsFalse_WhenNotLiked() {
        Long userId = 1L;
        Long movieId = 100L;

        LikedMoviesID id = new LikedMoviesID(userId, movieId);

        when(likedMovieRepository.findById(id)).thenReturn(Optional.empty());

        Boolean result = likedMovieService.isLiked(userId, movieId);

        assertFalse(result);
        verify(likedMovieRepository).findById(id);
    }

    @Test
    void testIsLiked_ReturnsFalse_WhenDeleted() {
        Long userId = 1L;
        Long movieId = 100L;

        LikedMoviesID id = new LikedMoviesID(userId, movieId);
        LikedMovie deletedLike = createLikedMovie(userId, movieId, "Inception", true);

        when(likedMovieRepository.findById(id)).thenReturn(Optional.of(deletedLike));

        Boolean result = likedMovieService.isLiked(userId, movieId);

        assertFalse(result);
        verify(likedMovieRepository).findById(id);
    }

    @Test
    void testIsLiked_DifferentMovies() {
        Long userId = 1L;
        Long movieId1 = 100L;
        Long movieId2 = 200L;

        LikedMoviesID id1 = new LikedMoviesID(userId, movieId1);
        LikedMoviesID id2 = new LikedMoviesID(userId, movieId2);

        LikedMovie likedMovie1 = createLikedMovie(userId, movieId1, "Movie 1", false);

        when(likedMovieRepository.findById(id1)).thenReturn(Optional.of(likedMovie1));
        when(likedMovieRepository.findById(id2)).thenReturn(Optional.empty());

        Boolean result1 = likedMovieService.isLiked(userId, movieId1);
        Boolean result2 = likedMovieService.isLiked(userId, movieId2);

        assertTrue(result1);
        assertFalse(result2);
    }

    // =============== Integration/Workflow Tests ===============
    @Test
    void testLikeUnlikeLikeWorkflow() {
        Long userId = 1L;
        Long movieId = 100L;
        String movieName = "Inception";

        User user = createUser(userId, true);
        Movie movie = createMovie(movieId, movieName);
        LikedMoviesID id = new LikedMoviesID(userId, movieId);

        // First like (new)
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(likedMovieRepository.findById(id)).thenReturn(Optional.empty());

        LikedMovie newLike = createLikedMovie(userId, movieId, movieName, false);
        when(likedMovieRepository.save(any(LikedMovie.class))).thenReturn(newLike);

        LikedMovie result1 = likedMovieService.likeMovie(userId, movieId);
        assertNotNull(result1);
        assertFalse(result1.getIsDeleted());

        // Unlike
        when(likedMovieRepository.findById(id)).thenReturn(Optional.of(newLike));
        likedMovieService.unlikeMovie(userId, movieId);
        assertTrue(newLike.getIsDeleted());

        // Like again (restore)
        when(likedMovieRepository.findById(id)).thenReturn(Optional.of(newLike));
        LikedMovie result2 = likedMovieService.likeMovie(userId, movieId);
        assertFalse(result2.getIsDeleted());
    }

    @Test
    void testMultipleUsersLikeSameMovie() {
        Long user1Id = 1L;
        Long user2Id = 2L;
        Long movieId = 100L;
        String movieName = "Inception";

        User user1 = createUser(user1Id, true);
        User user2 = createUser(user2Id, true);
        Movie movie = createMovie(movieId, movieName);

        LikedMoviesID id1 = new LikedMoviesID(user1Id, movieId);
        LikedMoviesID id2 = new LikedMoviesID(user2Id, movieId);

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));
        when(likedMovieRepository.findById(id1)).thenReturn(Optional.empty());
        when(likedMovieRepository.findById(id2)).thenReturn(Optional.empty());
        when(likedMovieRepository.save(any(LikedMovie.class))).thenReturn(new LikedMovie());

        likedMovieService.likeMovie(user1Id, movieId);
        likedMovieService.likeMovie(user2Id, movieId);

        verify(likedMovieRepository, times(2)).save(any(LikedMovie.class));
    }

    @Test
    void testUserLikesMultipleMovies() {
        Long userId = 1L;
        Long movieId1 = 100L;
        Long movieId2 = 200L;

        User user = createUser(userId, true);
        Movie movie1 = createMovie(movieId1, "Movie 1");
        Movie movie2 = createMovie(movieId2, "Movie 2");

        LikedMoviesID id1 = new LikedMoviesID(userId, movieId1);
        LikedMoviesID id2 = new LikedMoviesID(userId, movieId2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(movieRepository.findById(movieId1)).thenReturn(Optional.of(movie1));
        when(movieRepository.findById(movieId2)).thenReturn(Optional.of(movie2));
        when(likedMovieRepository.findById(id1)).thenReturn(Optional.empty());
        when(likedMovieRepository.findById(id2)).thenReturn(Optional.empty());
        when(likedMovieRepository.save(any(LikedMovie.class))).thenReturn(new LikedMovie());

        likedMovieService.likeMovie(userId, movieId1);
        likedMovieService.likeMovie(userId, movieId2);

        verify(likedMovieRepository, times(2)).save(any(LikedMovie.class));
    }

    @Test
    void testPrivacyEnforcement_MyLikedVsOtherUserLiked() {
        Long publicUserId = 1L;
        Long privateUserId = 2L;
        Pageable pageable = PageRequest.of(0, 20);

        User publicUser = createUser(publicUserId, true);
        User privateUser = createUser(privateUserId, false);

        Page<LikedMovieView> page = new PageImpl<>(Arrays.asList());

        // My liked movies - no privacy check needed
        when(likedMovieRepository.findAllByUserIdAndIsDeletedFalse(anyLong(), any(Pageable.class)))
                .thenReturn(page);

        Page<LikedMovieView> myLikes = likedMovieService.getMyLikedMovies(publicUserId, pageable);
        assertNotNull(myLikes);
        verifyNoInteractions(userRepository);

        // Other user's liked movies - public profile allowed
        when(userRepository.findById(publicUserId)).thenReturn(Optional.of(publicUser));
        Page<LikedMovieView> publicLikes = likedMovieService.getOtherUserLikedMovies(publicUserId, pageable);
        assertNotNull(publicLikes);

        // Other user's liked movies - private profile blocked
        when(userRepository.findById(privateUserId)).thenReturn(Optional.of(privateUser));
        assertThrows(PrivateProfileException.class,
                () -> likedMovieService.getOtherUserLikedMovies(privateUserId, pageable));
    }
}