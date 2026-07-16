package org.example.backend.movieReview;

import org.example.backend.errorHandler.ResourceNotFoundException;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.PrivateProfileException;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MovieReviewServiceAdditionalTest {

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
    private MovieReviewID reviewID;

    @BeforeEach
    void setup() {
        userId  = 1L;
        movieId = 100L;
        reviewID = new MovieReviewID(movieId, userId);

        user = new User();
        user.setId(userId);
        user.setIsPublic(true);

        movie = new Movie();
        movie.setMovieID(movieId);
        movie.setRatingCount(1);
        movie.setRatingSum(5L);
        movie.setAverageRating(5.0);
    }

    // -------------------------------------------------------------------------
    // TEST: deleteReview – deleting the last review resets avg and sum to zero
    // -------------------------------------------------------------------------
    @Test
    void deleteReview_LastReview_ResetsAverageAndSumToZero() {
        // Single review with rating 5; after deletion ratingCount drops to 0.
        MovieReview review = MovieReview.builder()
                .movieReviewID(reviewID)
                .movie(movie)
                .reviewer(user)
                .rating(5)
                .build();

        when(reviewRepository.findById(reviewID)).thenReturn(Optional.of(review));
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);

        reviewService.deleteReview(userId, movieId);

        assertEquals(0,   movie.getRatingCount(), "ratingCount must be 0 after last review is deleted");
        assertEquals(0L,  movie.getRatingSum(),   "ratingSum must be reset to 0");
        assertEquals(0.0, movie.getAverageRating(), "averageRating must be reset to 0.0");
        verify(reviewRepository).delete(review);
    }

    // -------------------------------------------------------------------------
    // TEST: deleteReview – review not found throws ResourceNotFoundException
    // -------------------------------------------------------------------------
    @Test
    void deleteReview_ReviewNotFound_ThrowsResourceNotFoundException() {
        when(reviewRepository.findById(reviewID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> reviewService.deleteReview(userId, movieId));

        assertEquals("Review not found", ex.getMessage());
        verify(movieRepository, never()).save(any());
        verify(reviewRepository, never()).delete(any(MovieReview.class));
    }

    // -------------------------------------------------------------------------
    // TEST: getOtherUserMovieReviews – private profile throws PrivateProfileException
    // -------------------------------------------------------------------------
    @Test
    void getOtherUserMovieReviews_PrivateProfile_ThrowsPrivateProfileException() {
        user.setIsPublic(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        PrivateProfileException ex = assertThrows(PrivateProfileException.class,
                () -> reviewService.getOtherUserMovieReviews(userId, Pageable.unpaged()));

        assertEquals("this profile is private", ex.getMessage());
        verify(reviewRepository, never()).findAllByReviewer_Id(anyLong(), any());
    }

    // -------------------------------------------------------------------------
    // TEST: getOtherUserMovieReviews – user not found throws ResourceNotFoundException
    // -------------------------------------------------------------------------
    @Test
    void getOtherUserMovieReviews_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getOtherUserMovieReviews(userId, Pageable.unpaged()));

        assertEquals("User not found", ex.getMessage());
        verify(reviewRepository, never()).findAllByReviewer_Id(anyLong(), any());
    }

    // -------------------------------------------------------------------------
    // TEST: addOrUpdateReview – average is rounded to one decimal place
    //       Scenario: 3 reviews with ratings 1, 1, 2  →  avg = 4/3 ≈ 1.3
    // -------------------------------------------------------------------------
    @Test
    void addOrUpdateReview_RatingRoundingToOneDecimal() {
        // Pre-existing state: 2 reviews rated 1 + 1 = 2 total.
        movie.setRatingCount(2);
        movie.setRatingSum(2L);

        // New review adds rating 2.
        MovieReviewDTO dto = new MovieReviewDTO(movieId, "decent", 2);

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

        MovieReviewDetailsDTO result = reviewService.addOrUpdateReview(userId, dto);

        assertNotNull(result);
        // ratingSum = 2 + 2 = 4, ratingCount = 3 → avg = 4/3 ≈ 1.333… → rounded = 1.3
        assertEquals(3,   movie.getRatingCount());
        assertEquals(4L,  movie.getRatingSum());
        assertEquals(1.3, movie.getAverageRating(), 0.0001,
                "Average of [1,1,2] should round to 1.3 with one decimal place");
    }
}
