package org.example.backend.watchHistory;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchHistoryControllerTest {

    @Mock
    private WatchHistoryService watchHistoryService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private WatchHistoryController watchHistoryController;

    private Long userId;
    private Long movieId;
    private WatchHistory watchHistory;

    @BeforeEach
    void setup() {
        userId = 1L;
        movieId = 100L;

        watchHistory = new WatchHistory();
        watchHistory.setId(1L);
        watchHistory.setUser(null);  // can set a user object if needed
        watchHistory.setMovie(null); // can set a movie object if needed
    }

    // -------------------------------------------------------------------------
    // TEST: Movie not found (service throws)
    // -------------------------------------------------------------------------
    @Test
    void testAddWatchHistoryMovieNotFound() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(watchHistoryService.addToWatchHistory(userId, movieId))
                .thenThrow(new RuntimeException("Movie not found"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> watchHistoryController.likeMovie(request, movieId));

        assertEquals("Movie not found", exception.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: User not found (service throws)
    // -------------------------------------------------------------------------
    @Test
    void testAddWatchHistoryUserNotFound() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(watchHistoryService.addToWatchHistory(userId, movieId))
                .thenThrow(new RuntimeException("User not found"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> watchHistoryController.likeMovie(request, movieId));

        assertEquals("User not found", exception.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: Successful addition
    // -------------------------------------------------------------------------
    @Test
    void testAddWatchHistorySuccess() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(watchHistoryService.addToWatchHistory(userId, movieId)).thenReturn(watchHistory);

        ResponseEntity<WatchHistory> response = watchHistoryController.likeMovie(request, movieId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(watchHistory, response.getBody());
    }
}
