package org.example.backend.requests;

import org.example.backend.movie.Movie;
import org.example.backend.organization.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestsServiceTest {

    @Mock
    private RequestsRepository requestsRepository;

    @InjectMocks
    private RequestsService requestsService;

    private Movie movie;
    private Organization organization;
    private Requests request;

    @BeforeEach
    void setUp() {
        organization = Organization.builder()
                .id(1L)
                .name("Org1")
                .email("org@example.com")
                .password("pass")
                .build();

        movie = Movie.builder()
                .movieID(10L)
                .name("Test Movie")
                .organization(organization)
                .build();

        request = Requests.builder()
                .id(1L)
                .movie(movie)
                .movieName(movie.getName())
                .organization(organization)
                .state(State.PENDING)
                .build();
    }

    // -----------------------------------------------------
    // TEST: addRequest()
    // -----------------------------------------------------
    @Test
    void testAddRequest() {
        when(requestsRepository.save(any(Requests.class))).thenReturn(request);

        Requests saved = requestsService.addRequest(movie);

        assertNotNull(saved);
        assertEquals(State.PENDING, saved.getState());
        assertEquals(movie, saved.getMovie());
        assertEquals(movie.getOrganization(), saved.getOrganization());

        verify(requestsRepository).save(any(Requests.class));
    }

    // -----------------------------------------------------
    // TEST: getAllPendingRequests()
    // -----------------------------------------------------
    @Test
    void testGetAllPendingRequests() {
        when(requestsRepository.findAllByState(State.PENDING)).thenReturn(List.of(request));

        List<Requests> pending = requestsService.getAllPendingRequests();

        assertEquals(1, pending.size());
        assertEquals(State.PENDING, pending.get(0).getState());
        verify(requestsRepository).findAllByState(State.PENDING);
    }

    // -----------------------------------------------------
    // TEST: getAllOrganizationRequests()
    // -----------------------------------------------------
    @Test
    void testGetAllOrganizationRequests() {
        when(requestsRepository.findAllByOrganization_Id(1L)).thenReturn(List.of(request));

        List<Requests> orgRequests = requestsService.getAllOrganizationRequests(1L);

        assertEquals(1, orgRequests.size());
        assertEquals(organization, orgRequests.get(0).getOrganization());
        verify(requestsRepository).findAllByOrganization_Id(1L);
    }

    // -----------------------------------------------------
    // TEST: deleteOldRequests()
    // -----------------------------------------------------
    @Test
    void testDeleteOldRequests() {
        requestsService.deleteOldRequests();

        // We can only verify that the repository method is called with a cutoff
        verify(requestsRepository).deleteOldNonPending(any(LocalDateTime.class));
    }
}
