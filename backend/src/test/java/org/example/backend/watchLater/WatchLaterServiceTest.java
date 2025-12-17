package org.example.backend.watchLater;

import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class WatchLaterServiceTest {

    @Mock
    private WatchLaterRepository watchLaterRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WatchLaterService watchLaterService;

    private Long userId;
    private Long movieId;
    private User user;
    private Movie movie;
    private WatchLaterID watchLaterID;

    @BeforeEach
    void setup() {
        userId = 1L;
        movieId = 10L;

        user = new User();
        user.setId(userId);

        movie = new Movie();
        movie.setMovieID(movieId);

        watchLaterID = new WatchLaterID(userId, movieId);
    }

    // -------------------------------------------------------------------------
    // TEST: Movie not found
    // -------------------------------------------------------------------------
    @Test
    void testAddMovieMovieNotFound() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> watchLaterService.addMovie(userId, movieId));

        assertEquals("Movie not found", exception.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: User not found
    // -------------------------------------------------------------------------
    @Test
    void testAddMovieUserNotFound() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> watchLaterService.addMovie(userId, movieId));

        assertEquals("User not found", exception.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: WatchLater already exists
    // -------------------------------------------------------------------------
    @Test
    void testAddMovieAlreadyExists() {
        WatchLater existing = WatchLater.builder()
                .watchLaterID(watchLaterID)
                .user(user)
                .movie(movie)
                .build();

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(watchLaterRepository.findById(watchLaterID)).thenReturn(Optional.of(existing));

        WatchLater result = watchLaterService.addMovie(userId, movieId);

        assertEquals(existing, result);
        verify(watchLaterRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // TEST: WatchLater does not exist → create new
    // -------------------------------------------------------------------------
    @Test
    void testAddMovieCreateNew() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(watchLaterRepository.findById(watchLaterID)).thenReturn(Optional.empty());

        WatchLater newWatchLater = WatchLater.builder()
                .watchLaterID(watchLaterID)
                .movie(movie)
                .user(user)
                .build();

        when(watchLaterRepository.save(any(WatchLater.class))).thenReturn(newWatchLater);

        WatchLater result = watchLaterService.addMovie(userId, movieId);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(movie, result.getMovie());
        assertEquals(watchLaterID, result.getWatchLaterID());

        verify(watchLaterRepository, times(1)).save(any(WatchLater.class));
    }
}
