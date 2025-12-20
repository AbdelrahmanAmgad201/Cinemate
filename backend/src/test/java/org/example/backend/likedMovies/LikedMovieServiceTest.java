package org.example.backend.likedMovies;

import org.example.backend.likedMovie.LikedMovie;
import org.example.backend.likedMovie.LikedMovieRepository;
import org.example.backend.likedMovie.LikedMovieService;
import org.example.backend.likedMovie.LikedMoviesID;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LikedMovieServiceTest {

    @Mock
    private LikedMovieRepository likedMovieRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LikedMovieService likedMovieService;

    private Long userId;
    private Long movieId;
    private User user;
    private Movie movie;
    private LikedMoviesID likedMoviesID;

    @BeforeEach
    void setup() {
        userId = 1L;
        movieId = 100L;

        user = new User();
        user.setId(userId);

        movie = new Movie();
        movie.setMovieID(movieId);

        likedMoviesID = new LikedMoviesID(userId, movieId);
    }

    // -------------------------------------------------------------------------
    // TEST: Movie not found
    // -------------------------------------------------------------------------
    @Test
    void testLikeMovieMovieNotFound() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> likedMovieService.likeMovie(userId, movieId));

        assertEquals("Movie not found", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: User not found
    // -------------------------------------------------------------------------
    @Test
    void testLikeMovieUserNotFound() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> likedMovieService.likeMovie(userId, movieId));

        assertEquals("User not found", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: Already liked
    // -------------------------------------------------------------------------
    @Test
    void testLikeMovieAlreadyExists() {
        LikedMovie existing = LikedMovie.builder()
                .likedMoviesID(likedMoviesID)
                .movieName(movie.getName())
                .user(user)
                .build();

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(likedMovieRepository.findById(likedMoviesID)).thenReturn(Optional.of(existing));

        LikedMovie result = likedMovieService.likeMovie(userId, movieId);

        assertEquals(existing, result);
        verify(likedMovieRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // TEST: New like → save new
    // -------------------------------------------------------------------------
    @Test
    void testLikeMovieCreateNew() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(likedMovieRepository.findById(likedMoviesID)).thenReturn(Optional.empty());

        LikedMovie newLikedMovie = LikedMovie.builder()
                .likedMoviesID(likedMoviesID)
                .movieName(movie.getName())
                .user(user)
                .build();

        when(likedMovieRepository.save(any(LikedMovie.class))).thenReturn(newLikedMovie);

        LikedMovie result = likedMovieService.likeMovie(userId, movieId);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(movie.getName(), result.getMovieName());
        assertEquals(likedMoviesID, result.getLikedMoviesID());

        verify(likedMovieRepository, times(1)).save(any(LikedMovie.class));
    }
}
