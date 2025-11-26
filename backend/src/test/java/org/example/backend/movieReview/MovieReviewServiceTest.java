package org.example.backend.movieReview;

import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieReviewServiceTest {

    @Mock
    private MovieReviewRepository reviewRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MovieReviewService reviewService;

    private Long userId;
    private Long movieId;
    private User user;
    private Movie movie;
    private MovieReviewDTO dto;
    private MovieReviewID reviewID;

    @BeforeEach
    void setup() {
        userId = 1L;
        movieId = 100L;

        user = new User();
        user.setId(userId);

        movie = new Movie();
        movie.setMovieID(movieId);
        movie.setRatingCount(0);
        movie.setRatingSum(0L);

        dto = new MovieReviewDTO();
        dto.setMovieId(movieId);
        dto.setRating(4);
        dto.setComment("Great movie!");

        reviewID = new MovieReviewID(movieId, userId);
    }

    // -------------------------------------------------------------------------
    // TEST: Movie not found
    // -------------------------------------------------------------------------
    @Test
    void testAddOrUpdateReviewMovieNotFound() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.addOrUpdateReview(userId, dto));

        assertEquals("Movie not found", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: User not found
    // -------------------------------------------------------------------------
    @Test
    void testAddOrUpdateReviewUserNotFound() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.addOrUpdateReview(userId, dto));

        assertEquals("User not found", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: New review
    // -------------------------------------------------------------------------
    @Test
    void testAddOrUpdateReviewNew() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(reviewRepository.existsById(reviewID)).thenReturn(false);
        when(reviewRepository.findById(reviewID)).thenReturn(Optional.empty());

        MovieReview savedReview = MovieReview.builder()
                .movieReviewID(reviewID)
                .movie(movie)
                .reviewer(user)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .build();

        when(reviewRepository.save(any(MovieReview.class))).thenReturn(savedReview);
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);

        MovieReview result = reviewService.addOrUpdateReview(userId, dto);

        assertNotNull(result);
        assertEquals(dto.getRating(), result.getRating());
        assertEquals(dto.getComment(), result.getComment());

        assertEquals(1, movie.getRatingCount());
        assertEquals(4L, movie.getRatingSum());
        assertEquals(4.0, movie.getAverageRating());
    }

    // -------------------------------------------------------------------------
    // TEST: Existing review update
    // -------------------------------------------------------------------------
    @Test
    void testAddOrUpdateReviewExisting() {
        movie.setRatingCount(2);
        movie.setRatingSum(7L); // e.g., previous ratings 3 + 4

        MovieReview existingReview = MovieReview.builder()
                .movieReviewID(reviewID)
                .movie(movie)
                .reviewer(user)
                .rating(3)
                .comment("Old comment")
                .build();

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(reviewRepository.existsById(reviewID)).thenReturn(true);
        when(reviewRepository.findById(reviewID)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(MovieReview.class))).thenReturn(existingReview);
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);

        MovieReview result = reviewService.addOrUpdateReview(userId, dto);

        assertNotNull(result);
        assertEquals(dto.getRating(), result.getRating());
        assertEquals(dto.getComment(), result.getComment());

        // ratingSum updated: 7 - 3 + 4 = 8
        assertEquals(2, movie.getRatingCount()); // count stays same
        assertEquals(8L, movie.getRatingSum());
        assertEquals(4.0, movie.getAverageRating());
    }
}
