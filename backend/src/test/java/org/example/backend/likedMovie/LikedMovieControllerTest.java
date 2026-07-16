package org.example.backend.likedMovie;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LikedMovieControllerTest {

    @Mock
    private LikedMovieService likedMovieService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private LikedMovieController likedMovieController;

    private Long userId;
    private Long movieId;
    private LikedMovieResponse likedMovie;

    @BeforeEach
    void setup() {
        userId = 1L;
        movieId = 100L;

        likedMovie = LikedMovieResponse.builder()
                .movieId(movieId)
                .build();
    }

    // -------------------------------------------------------------------------
    // TEST: Movie not found
    // -------------------------------------------------------------------------
    @Test
    void testLikeMovieMovieNotFound() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(likedMovieService.likeMovie(userId, movieId))
                .thenThrow(new RuntimeException("Movie not found"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> likedMovieController.likeMovie(request, movieId));

        assertEquals("Movie not found", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: User not found
    // -------------------------------------------------------------------------
    @Test
    void testLikeMovieUserNotFound() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(likedMovieService.likeMovie(userId, movieId))
                .thenThrow(new RuntimeException("User not found"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> likedMovieController.likeMovie(request, movieId));

        assertEquals("User not found", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: Like successful (already liked or new)
    // -------------------------------------------------------------------------
    @Test
    void testLikeMovieSuccess() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(likedMovieService.likeMovie(userId, movieId)).thenReturn(likedMovie);

        ResponseEntity<LikedMovieResponse> response = likedMovieController.likeMovie(request, movieId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(likedMovie, response.getBody());
    }
}
