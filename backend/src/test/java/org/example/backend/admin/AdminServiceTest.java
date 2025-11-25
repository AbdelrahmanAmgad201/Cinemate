package org.example.backend.admin;

import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsRepository;
import org.example.backend.requests.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private RequestsRepository requestsRepository;

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminService adminService;

    private Requests request;
    private Movie movie;
    private Admin admin;

    @BeforeEach
    void setUp() {
        movie = Movie.builder()
                .movieID(10L)
                .name("Test Movie")
                .build();
        admin = Admin.builder()
                .id(5L)
                .name("Admin User")
                .build();

        request = Requests.builder()
                .id(1L)
                .movie(movie)
                .state(State.PENDING)
                .build();
    }

    // -----------------------------------------------------
    // TEST: getRequestedMovie()
    // -----------------------------------------------------
    @Test
    void testGetRequestedMovie() {
        when(requestsRepository.findById(1L)).thenReturn(Optional.of(request));

        Movie result = adminService.getRequestedMovie(1L);

        assertEquals(movie, result);
        verify(requestsRepository).findById(1L);
    }

    // -----------------------------------------------------
    // TEST: acceptRequests()
    // -----------------------------------------------------
    @Test
    void testAcceptRequests() {
        AcceptDTO dto = new AcceptDTO();
        dto.setRequestId(1L);
        dto.setAdminId(5L);

        when(adminRepository.findById(5L)).thenReturn(Optional.of(admin));
        when(requestsRepository.findById(1L)).thenReturn(Optional.of(request));

        adminService.acceptRequests(dto);

        assertEquals(State.ACCEPTED, request.getState());
        assertNotNull(request.getStateUpdatedAt());
        assertEquals(admin, movie.getAdmin());

        verify(adminRepository).findById(5L);
        verify(requestsRepository).findById(1L);
        verify(requestsRepository).save(request);
        verify(movieRepository).save(movie);
    }

    // -----------------------------------------------------
    // TEST: declineRequest()
    // -----------------------------------------------------
    @Test
    void testDeclineRequest() {
        when(requestsRepository.findById(1L)).thenReturn(Optional.of(request));

        adminService.declineRequest(1L);

        assertEquals(State.REJECTED, request.getState());
        assertNull(request.getMovie());
        assertNotNull(request.getStateUpdatedAt());

        verify(requestsRepository).save(request);
        verify(movieRepository).deleteById(10L);
    }
}
