package org.example.backend.watchHistory;

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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchHistoryServiceTest {

    @Mock
    private WatchHistoryRepository watchHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private WatchHistoryService watchHistoryService;

    private Long userId;
    private Long movieId;
    private User user;
    private Movie movie;

    @BeforeEach
    void setup() {
        userId = 1L;
        movieId = 100L;

        user = new User();
        user.setId(userId);

        movie = new Movie();
        movie.setMovieID(movieId);
    }

    // -------------------------------------------------------------------------
    // TEST: Movie not found
    // -------------------------------------------------------------------------
    @Test
    void testAddToWatchHistoryMovieNotFound() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> watchHistoryService.addToWatchHistory(userId, movieId));

        assertEquals("Movie not found", exception.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: User not found
    // -------------------------------------------------------------------------
    @Test
    void testAddToWatchHistoryUserNotFound() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> watchHistoryService.addToWatchHistory(userId, movieId));

        assertEquals("User not found", exception.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: Successful addition to WatchHistory
    // -------------------------------------------------------------------------
    @Test
    void testAddToWatchHistorySuccess() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        WatchHistory savedWatchHistory = new WatchHistory();
        savedWatchHistory.setUser(user);
        savedWatchHistory.setMovie(movie);

        when(watchHistoryRepository.save(any(WatchHistory.class)))
                .thenReturn(savedWatchHistory);

        WatchHistory result = watchHistoryService.addToWatchHistory(userId, movieId);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(movie, result.getMovie());

        verify(watchHistoryRepository, times(1)).save(any(WatchHistory.class));
    }
}
