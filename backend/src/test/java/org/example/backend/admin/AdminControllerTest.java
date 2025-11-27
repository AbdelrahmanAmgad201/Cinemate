package org.example.backend.admin;

import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieService;
import org.example.backend.movie.OneMovieOverView;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import jakarta.servlet.http.HttpServletRequest;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private RequestsService requestsService;

    @Mock
    private MovieService movieService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAllAdminRequests() {
        Long adminId = 1L;
        Requests req1 = mock(Requests.class);
        Requests req2 = mock(Requests.class);
        when(requestsService.getAllAdminRequests(adminId)).thenReturn(List.of(req1, req2));
        when(httpServletRequest.getAttribute("userId")).thenReturn(adminId);
        ResponseEntity<List<Requests>> response = adminController.findAllAdminRequests(httpServletRequest);

        assertEquals(2, response.getBody().size());
        verify(requestsService, times(1)).getAllAdminRequests(adminId);
    }

    @Test
    void testDeclineRequest_Success() {
        Long requestId = 5L;
        Long adminId = 1L;
        when(httpServletRequest.getAttribute("userId")).thenReturn(adminId);

        ResponseEntity<String> response = adminController.declineRequest(httpServletRequest, requestId);

        verify(adminService, times(1)).declineRequest(adminId, requestId);
        assertTrue(response.getBody().contains("declined successfully"));
    }

    @Test
    void testDeclineRequest_Failure() {
        Long requestId = 5L;
        Long adminId = 1L;
        when(httpServletRequest.getAttribute("userId")).thenReturn(adminId);
        doThrow(new RuntimeException("Movie not found")).when(adminService).declineRequest(adminId, requestId);

        ResponseEntity<String> response = adminController.declineRequest(httpServletRequest, requestId);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("Failed to decline request"));
    }

    @Test
    void testAcceptRequest_Success() {
        Long requestId = 10L;
        Long adminId = 2L;
        when(httpServletRequest.getAttribute("userId")).thenReturn(adminId);

        ResponseEntity<String> response = adminController.acceptRequest(httpServletRequest, requestId);

        verify(adminService, times(1)).acceptRequests(adminId, requestId);
        assertTrue(response.getBody().contains("accepted successfully"));
    }

    @Test
    void testAcceptRequest_Failure() {
        Long requestId = 10L;
        Long adminId = 2L;
        when(httpServletRequest.getAttribute("userId")).thenReturn(adminId);
        doThrow(new RuntimeException("Movie not found")).when(adminService).acceptRequests(adminId, requestId);

        ResponseEntity<String> response = adminController.acceptRequest(httpServletRequest, requestId);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("Failed to accept request"));
    }

    @Test
    void testFindAllPendingRequests() {
        Requests r1 = mock(Requests.class);
        Requests r2 = mock(Requests.class);
        when(requestsService.getAllPendingRequests()).thenReturn(List.of(r1, r2));

        ResponseEntity<List<Requests>> response = adminController.findAllPendingRequests(httpServletRequest);

        assertEquals(2, response.getBody().size());
        verify(requestsService, times(1)).getAllPendingRequests();
    }

    @Test
    void testGetRequestedMovie() {
        Long requestId = 3L;
        Movie movie = mock(Movie.class);
        when(adminService.getRequestedMovie(requestId)).thenReturn(movie);

        ResponseEntity<Movie> response = adminController.getRequestedMovie(httpServletRequest, requestId);

        assertEquals(movie, response.getBody());
        verify(adminService, times(1)).getRequestedMovie(requestId);
    }

    @Test
    void testGetSpecificMovieOverview() {
        Long movieId = 7L;
        OneMovieOverView overview = mock(OneMovieOverView.class);
        when(movieService.getMovieStatsByMovieId(movieId)).thenReturn(overview);

        ResponseEntity<OneMovieOverView> response = adminController.getSpecificMovieOverview(httpServletRequest, movieId);

        assertEquals(overview, response.getBody());
        verify(movieService, times(1)).getMovieStatsByMovieId(movieId);
    }

    @Test
    void testGetSystemOverview() {
        SystemOverview overview = mock(SystemOverview.class);
        when(adminService.getSystemOverview()).thenReturn(overview);

        ResponseEntity<SystemOverview> response = adminController.getSystemOverview(httpServletRequest);

        assertEquals(overview, response.getBody());
        verify(adminService, times(1)).getSystemOverview();
    }

    @Test
    void testAllBranches_EmptyLists() {
        // Simulate empty lists returned from services
        when(requestsService.getAllAdminRequests(1L)).thenReturn(Collections.emptyList());
        when(requestsService.getAllPendingRequests()).thenReturn(Collections.emptyList());
        when(httpServletRequest.getAttribute("userId")).thenReturn(1L);
        ResponseEntity<List<Requests>> adminRequests = adminController.findAllAdminRequests(httpServletRequest);
        ResponseEntity<List<Requests>> pendingRequests = adminController.findAllPendingRequests(httpServletRequest);

        assertTrue(adminRequests.getBody().isEmpty());
        assertTrue(pendingRequests.getBody().isEmpty());
    }
}
