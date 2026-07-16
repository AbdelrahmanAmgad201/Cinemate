package org.example.backend.organization;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieAddDTO;
import org.example.backend.movie.MovieService;
import org.example.backend.movie.OneMovieOverView;
import org.example.backend.requests.RequestsResponse;
import org.example.backend.requests.RequestsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class OrganizationControllerTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private RequestsService requestsService;

    @Mock
    private MovieService movieService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private OrganizationController organizationController;

    private OrganizationDataDTO organizationDataDTO;
    private MovieAddDTO movieAddDTO;
    private RequestsResponse request1;
    private RequestsResponse request2;
    private Movie movie1;
    private Movie movie2;
    private MoviesOverview moviesOverview;
    private OneMovieOverView oneMovieOverview;

    @BeforeEach
    void setup() {
        organizationDataDTO = new OrganizationDataDTO();
        movieAddDTO = new MovieAddDTO();

        request1 = RequestsResponse.builder().build();
        request2 = RequestsResponse.builder().build();

        movie1 = new Movie();
        movie2 = new Movie();

        moviesOverview = new MoviesOverview(5, 200, null);

        oneMovieOverview = new OneMovieOverView(
                100L,   // views
                50L,    // watchers
                20L,    // likes
                10L,    // num ratings
                4.5,   // avg rating
                15L     // watch later
        );
    }

    // -------------------------------------------------------------------------
    // TEST: /profile
    // -------------------------------------------------------------------------
    @Test
    void testGetProfile() {
        when(request.getAttribute("userId")).thenReturn(1L);
        when(request.getAttribute("userEmail")).thenReturn("test@example.com");

        ResponseEntity<?> response = organizationController.getMyProfile(request);

        assertEquals("User profile for ID: 1, Email: test@example.com", response.getBody());
    }

    // -------------------------------------------------------------------------
    // TEST: PUT /v1/profile
    // -------------------------------------------------------------------------
    @Test
    void testSetOrganizationData() {
        when(request.getAttribute("userId")).thenReturn(1L);
        when(organizationService.updateOrganizationData(1L, organizationDataDTO))
                .thenReturn("Organization data updated");

        ResponseEntity<String> response =
                organizationController.setPersonalData(request, organizationDataDTO);

        assertEquals("Organization data updated", response.getBody());
    }

    // -------------------------------------------------------------------------
    // TEST: POST /v1/movies (success)
    // -------------------------------------------------------------------------
    @Test
    void testAddMovieSuccess() {
        when(request.getAttribute("userId")).thenReturn(1L);
        when(organizationService.requestMovie(1L, movieAddDTO)).thenReturn(99L);

        ResponseEntity<?> res = organizationController.addMovie(request, movieAddDTO);

        Map<String, Object> body = (Map<String, Object>) res.getBody();

        assertEquals(true, body.get("success"));
        assertEquals("Movie request submitted successfully", body.get("message"));
        assertEquals(99L, body.get("movieId"));
    }

    // -------------------------------------------------------------------------
    // TEST: POST /v1/movies (failure)
    // -------------------------------------------------------------------------
    @Test
    void testAddMovieFailure() {
        when(request.getAttribute("userId")).thenReturn(1L);
        when(organizationService.requestMovie(1L, movieAddDTO))
                .thenThrow(new RuntimeException("Error!!"));

        // No manual try/catch in the controller anymore (API-NEW-03) — the exception
        // propagates to GlobalExceptionHandler instead of being caught here.
        assertThrows(RuntimeException.class,
                () -> organizationController.addMovie(request, movieAddDTO));
    }

    // -------------------------------------------------------------------------
    // TEST: /v1/requests
    // -------------------------------------------------------------------------
    @Test
    void testGetOrganizationRequests() {
        when(request.getAttribute("userId")).thenReturn(1L);
        when(requestsService.getAllOrganizationRequests(1L))
                .thenReturn(List.of(request1, request2));

        ResponseEntity<List<RequestsResponse>> res = organizationController.getOrgRequests(request);

        assertEquals(2, res.getBody().size());
    }

    // -------------------------------------------------------------------------
    // TEST: /movies-overview
    // -------------------------------------------------------------------------
    @Test
    void testGetMoviesOverview() {
        when(request.getAttribute("userId")).thenReturn(1L);
        when(movieService.getMoviesOverview(1L)).thenReturn(moviesOverview);

        ResponseEntity<MoviesOverview> res = organizationController.getMoviesOverview(request);

        assertEquals(200, res.getStatusCode().value());
        assertEquals(moviesOverview, res.getBody());
    }

    // -------------------------------------------------------------------------
    // TEST: /v1/movies/{movieId}/overview (authorized)
    // -------------------------------------------------------------------------
    @Test
    void testGetSpecificMovieOverviewAuthorized() {
        when(request.getAttribute("userId")).thenReturn(1L);
        when(movieService.OrganizationOwnMovie(1L, 10L)).thenReturn(true);
        when(movieService.getMovieStatsByMovieId(10L)).thenReturn(oneMovieOverview);

        ResponseEntity<OneMovieOverView> res =
                organizationController.getSpecificMovieOverview(request, 10L);

        assertEquals(200, res.getStatusCode().value());
        assertEquals(oneMovieOverview, res.getBody());
    }

    // -------------------------------------------------------------------------
    // TEST: /v1/movies/{movieId}/overview (forbidden)
    // -------------------------------------------------------------------------
    @Test
    void testGetSpecificMovieOverviewForbidden() {
        when(request.getAttribute("userId")).thenReturn(1L);
        when(movieService.OrganizationOwnMovie(1L, 10L)).thenReturn(false);

        ResponseEntity<OneMovieOverView> res =
                organizationController.getSpecificMovieOverview(request, 10L);

        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }
    // -------------------------------------------------------------------------
// TEST: /v1/requests-overview
// -------------------------------------------------------------------------
    @Test
    void testGetRequestsOverview() {
        when(request.getAttribute("userId")).thenReturn(1L);

        // Mock service response
        RequestsOverView overview = RequestsOverView.builder()
                .numberOfPendings(3L)
                .numberOfRejected(2L)
                .numberOfAccepted(5L)
                .build();

        when(requestsService.getRequestsOverView(1L)).thenReturn(overview);

        ResponseEntity<RequestsOverView> res =
                organizationController.getRequestsOverview(request);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(3L, res.getBody().getNumberOfPendings());
        assertEquals(2L, res.getBody().getNumberOfRejected());
        assertEquals(5L, res.getBody().getNumberOfAccepted());
    }

}
