package org.example.backend.watchLater;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class WatchLaterControllerTest {

    @Mock
    private WatchLaterService watchLaterService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private WatchLaterController watchLaterController;

    private Long userId;
    private Long movieId;
    private WatchLaterID watchLaterID;
    private WatchLater watchLater;
    private WatchLater newWatchLater;

    @BeforeEach
    void setup() {
        userId = 1L;
        movieId = 10L;
        watchLaterID = new WatchLaterID(userId, movieId);

        watchLater = WatchLater.builder()
                .watchLaterID(watchLaterID)
                .build();

        newWatchLater = WatchLater.builder()
                .watchLaterID(watchLaterID)
                .build();
    }

    // -------------------------------------------------------------------------
    // TEST: Movie not found (service throws)
    // -------------------------------------------------------------------------
    @Test
    void testAddToWatchLaterMovieNotFound() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(watchLaterService.addMovie(userId, movieId))
                .thenThrow(new RuntimeException("Movie not found"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> watchLaterController.addToWatchLater(request, movieId));

        assertEquals("Movie not found", exception.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: User not found (service throws)
    // -------------------------------------------------------------------------
    @Test
    void testAddToWatchLaterUserNotFound() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(watchLaterService.addMovie(userId, movieId))
                .thenThrow(new RuntimeException("User not found"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> watchLaterController.addToWatchLater(request, movieId));

        assertEquals("User not found", exception.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: WatchLater already exists
    // -------------------------------------------------------------------------
    @Test
    void testAddToWatchLaterAlreadyExists() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(watchLaterService.addMovie(userId, movieId)).thenReturn(watchLater);

        ResponseEntity<WatchLater> response = watchLaterController.addToWatchLater(request, movieId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(watchLater, response.getBody());
    }

    // -------------------------------------------------------------------------
    // TEST: WatchLater created new
    // -------------------------------------------------------------------------
    @Test
    void testAddToWatchLaterCreateNew() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(watchLaterService.addMovie(userId, movieId)).thenReturn(newWatchLater);

        ResponseEntity<WatchLater> response = watchLaterController.addToWatchLater(request, movieId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(newWatchLater, response.getBody());
    }
}
