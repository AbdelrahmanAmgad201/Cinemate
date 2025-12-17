package org.example.backend.movieReview;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MovieReviewControllerTest {

    @Mock
    private MovieReviewService movieReviewService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private MovieReviewController movieReviewController;

    private Long userId;
    private MovieReviewDTO dto;
    private MovieReviewID reviewID;
    private MovieReview review;

    @BeforeEach
    void setup() {
        userId = 1L;

        dto = new MovieReviewDTO();
        dto.setMovieId(100L);
        dto.setRating(5);
        dto.setComment("Amazing!");

        reviewID = new MovieReviewID(dto.getMovieId(), userId);

        review = MovieReview.builder()
                .movieReviewID(reviewID)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .build();
    }

    // -------------------------------------------------------------------------
    // TEST: addOrUpdateReview → Movie not found
    // -------------------------------------------------------------------------
    @Test
    void testAddOrUpdateReviewMovieNotFound() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(movieReviewService.addOrUpdateReview(userId, dto))
                .thenThrow(new RuntimeException("Movie not found"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> movieReviewController.addOrUpdateReview(request, dto));

        assertEquals("Movie not found", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: addOrUpdateReview → User not found
    // -------------------------------------------------------------------------
    @Test
    void testAddOrUpdateReviewUserNotFound() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(movieReviewService.addOrUpdateReview(userId, dto))
                .thenThrow(new RuntimeException("User not found"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> movieReviewController.addOrUpdateReview(request, dto));

        assertEquals("User not found", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // TEST: addOrUpdateReview → Success
    // -------------------------------------------------------------------------
    @Test
    void testAddOrUpdateReviewSuccess() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(movieReviewService.addOrUpdateReview(userId, dto)).thenReturn(review);

        ResponseEntity<MovieReview> response =
                movieReviewController.addOrUpdateReview(request, dto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(review, response.getBody());
    }

    // -------------------------------------------------------------------------
    // TEST: getMovieReviews → Success
    // -------------------------------------------------------------------------
    @Test
    void testGetMovieReviewsSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MovieReview> page = new PageImpl<>(List.of(review), pageable, 1);

        when(movieReviewService.getMovieReviews(dto.getMovieId(), pageable)).thenReturn(page);

        ResponseEntity<Page<MovieReview>> response =
                movieReviewController.getMovieReviews(dto.getMovieId(), pageable);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(review, response.getBody().getContent().get(0));
    }
}
