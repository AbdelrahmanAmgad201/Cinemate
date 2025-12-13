package org.example.backend.likedMovies;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.likedMovie.LikedMovie;
import org.example.backend.likedMovie.LikedMovieController;
import org.example.backend.likedMovie.LikedMovieService;
import org.example.backend.likedMovie.LikedMoviesID;
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
    private LikedMoviesID likedMoviesID;
    private LikedMovie likedMovie;

    @BeforeEach
    void setup() {
        userId = 1L;
        movieId = 100L;

        likedMoviesID = new LikedMoviesID(userId, movieId);

        likedMovie = LikedMovie.builder()
                .likedMoviesID(likedMoviesID)
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

        ResponseEntity<LikedMovie> response = likedMovieController.likeMovie(request, movieId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(likedMovie, response.getBody());
    }
}
