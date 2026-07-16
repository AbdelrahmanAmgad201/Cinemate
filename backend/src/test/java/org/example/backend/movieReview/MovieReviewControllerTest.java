package org.example.backend.movieReview;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.errorHandler.ResourceNotFoundException;
import org.example.backend.user.PrivateProfileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Refactored from bare @InjectMocks to @WebMvcTest so HTTP dispatch, @Valid constraints,
 * content-type negotiation, and GlobalExceptionHandler mappings are all exercised
 * (not just the controller method body in isolation).
 *
 * <p>Security filters are disabled (addFilters = false) since authorization is covered
 * separately. The userId request attribute that GatewayAuthenticationFilter would
 * normally set is supplied directly on each request instead.
 */
@Import(Jackson2AutoConfiguration.class)
@WebMvcTest(MovieReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MovieReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovieReviewService movieReviewService;

    private static final Long USER_ID = 1L;
    private static final Long MOVIE_ID = 100L;

    private MovieReviewDTO validReviewDto;
    private MovieReviewDetailsDTO reviewDetails;

    @BeforeEach
    void setUp() {
        validReviewDto = new MovieReviewDTO(MOVIE_ID, "A great film!", 8);

        reviewDetails = MovieReviewDetailsDTO.builder()
                .movieReviewID(new MovieReviewID(MOVIE_ID, USER_ID))
                .movie("The Dark Knight")
                .rating(8)
                .comment("A great film!")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/movie-review/v1 — addOrUpdateReview
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void addOrUpdateReview_ValidPayload_Returns200WithBody() throws Exception {
        when(movieReviewService.addOrUpdateReview(eq(USER_ID), any(MovieReviewDTO.class)))
                .thenReturn(reviewDetails);

        mockMvc.perform(post("/api/movie-review/v1")
                        .requestAttr("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReviewDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating", is(8)))
                .andExpect(jsonPath("$.comment", is("A great film!")));
    }

    @Test
    void addOrUpdateReview_NullMovieId_ReturnsBadRequestFromValidation() throws Exception {
        MovieReviewDTO dto = new MovieReviewDTO(null, "comment", 5);

        mockMvc.perform(post("/api/movie-review/v1")
                        .requestAttr("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addOrUpdateReview_RatingBelowMin_ReturnsBadRequestFromValidation() throws Exception {
        MovieReviewDTO dto = new MovieReviewDTO(MOVIE_ID, "comment", 0); // min is 1

        mockMvc.perform(post("/api/movie-review/v1")
                        .requestAttr("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addOrUpdateReview_RatingAboveMax_ReturnsBadRequestFromValidation() throws Exception {
        MovieReviewDTO dto = new MovieReviewDTO(MOVIE_ID, "comment", 11); // max is 10

        mockMvc.perform(post("/api/movie-review/v1")
                        .requestAttr("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addOrUpdateReview_MovieNotFound_Returns404() throws Exception {
        when(movieReviewService.addOrUpdateReview(eq(USER_ID), any(MovieReviewDTO.class)))
                .thenThrow(new ResourceNotFoundException("Movie not found"));

        mockMvc.perform(post("/api/movie-review/v1")
                        .requestAttr("userId", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReviewDto)))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/movie-review/v1/{movieId} — deleteReview
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void deleteReview_Success_Returns200() throws Exception {
        mockMvc.perform(delete("/api/movie-review/v1/{movieId}", MOVIE_ID)
                        .requestAttr("userId", USER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReview_ReviewNotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Review not found"))
                .when(movieReviewService).deleteReview(USER_ID, MOVIE_ID);

        mockMvc.perform(delete("/api/movie-review/v1/{movieId}", MOVIE_ID)
                        .requestAttr("userId", USER_ID))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/movie-review/v1/movie/{movieId} — getMovieReviews
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getMovieReviews_ReturnsPagedResults() throws Exception {
        Page<MovieReviewDetailsDTO> page = new PageImpl<>(
                List.of(reviewDetails), PageRequest.of(0, 10), 1);
        when(movieReviewService.getMovieReviews(eq(MOVIE_ID), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/movie-review/v1/movie/{movieId}", MOVIE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].rating", is(8)));
    }

    @Test
    void getMovieReviews_EmptyPage_Returns200WithEmptyContent() throws Exception {
        Page<MovieReviewDetailsDTO> empty = Page.empty();
        when(movieReviewService.getMovieReviews(eq(MOVIE_ID), any(Pageable.class)))
                .thenReturn(empty);

        mockMvc.perform(get("/api/movie-review/v1/movie/{movieId}", MOVIE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/movie-review/v1/my-reviews — getMyMovieReviews
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getMyMovieReviews_Returns200WithPagedResults() throws Exception {
        Page<MovieReviewDetailsDTO> page = new PageImpl<>(List.of(reviewDetails));
        when(movieReviewService.getMyMovieReviews(eq(USER_ID), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/movie-review/v1/my-reviews")
                        .requestAttr("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating", is(8)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/movie-review/v1/user/{userId} — getOtherUserMovieReviews
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getOtherUserMovieReviews_PublicProfile_Returns200() throws Exception {
        Page<MovieReviewDetailsDTO> page = new PageImpl<>(List.of(reviewDetails));
        when(movieReviewService.getOtherUserMovieReviews(eq(2L), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/movie-review/v1/user/{userId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating", is(8)));
    }

    @Test
    void getOtherUserMovieReviews_PrivateProfile_Returns403() throws Exception {
        when(movieReviewService.getOtherUserMovieReviews(eq(2L), any(Pageable.class)))
                .thenThrow(new PrivateProfileException("this profile is private"));

        mockMvc.perform(get("/api/movie-review/v1/user/{userId}", 2L))
                .andExpect(status().isForbidden());
    }
}
