package org.example.backend.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieService;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private MovieService movieService;

    @Mock
    private AdminService adminService;

    @Mock
    private RequestsService requestsService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AdminController adminController;

    private Movie movie1;
    private Movie movie2;
    private Requests request1;
    private Requests request2;
    private Admin admin1;

    @BeforeEach
    void setUp() {
        movie1 = Movie.builder()
                .movieID(1L)
                .name("Movie 1")
                .description("Description 1")
                .movieUrl("http://movie1.url")
                .thumbnailUrl("http://thumb1.url")
                .build();

        movie2 = Movie.builder()
                .movieID(2L)
                .name("Movie 2")
                .description("Description 2")
                .movieUrl("http://movie2.url")
                .thumbnailUrl("http://thumb2.url")
                .build();

        request1 = new Requests();
        request1.setId(1L);
        request1.setMovie(movie1);

        request2 = new Requests();
        request2.setId(2L);
        request2.setMovie(movie2);
        admin1 = Admin.builder()
                .id(1L)
                .name("Admin 1")
                .build();
    }

    @Test
    void testFindAllAdminRequests() {
        Long adminId = 1L;

        request1.setAdmin(admin1);
        request2.setAdmin(admin1);

        List<Requests> requests = Arrays.asList(request1, request2);
        when(requestsService.getAllAdminRequests(adminId)).thenReturn(requests);


        ResponseEntity<List<Requests>> response = adminController.findAllAdminRequests(httpRequest,adminId);

        assertEquals(2, response.getBody().size());
        verify(requestsService, times(1)).getAllAdminRequests(adminId);
    }

    @Test
    void testDeclineMovie() {

        when(httpRequest.getAttribute("userId")).thenReturn(1L);
        ResponseEntity<Void> response = adminController.declineMovie(httpRequest,1L);

        assertEquals(200, response.getStatusCodeValue());
        verify(adminService, times(1)).declineRequest(1L,1L);
    }

    @Test
    void testAcceptMovie() {

        when(httpRequest.getAttribute("userId")).thenReturn(1L);
        ResponseEntity<Void> response = adminController.acceptMovie(httpRequest,1L);

        assertEquals(200, response.getStatusCodeValue());
        verify(adminService, times(1)).acceptRequests(1L,1L);
    }

    @Test
    void testFindAllPendingRequests() {
        List<Requests> requests = Arrays.asList(request1, request2);
        when(requestsService.getAllPendingRequests()).thenReturn(requests);

        ResponseEntity<List<Requests>> response = adminController.findAllPendingRequests(httpRequest);

        assertEquals(2, response.getBody().size());
        verify(requestsService, times(1)).getAllPendingRequests();
    }

    @Test
    void testGetRequestedMovie() {
        Long requestId = 1L;

        when(adminService.getRequestedMovie(requestId)).thenReturn(movie1);

        ResponseEntity<Movie> response = adminController.getRequestedMovie(httpRequest,requestId);

        assertEquals(movie1, response.getBody());
        verify(adminService, times(1)).getRequestedMovie(requestId);
    }

}
