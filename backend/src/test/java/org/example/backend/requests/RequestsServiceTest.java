package org.example.backend.requests;

import org.example.backend.movie.Movie;
import org.example.backend.organization.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RequestsServiceTest {

    @Mock
    private RequestsRepository requestsRepository;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2024-06-15T12:00:00Z"), ZoneOffset.UTC);

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
        when(requestsRepository.findAllByState(eq(State.PENDING), any(Pageable.class)))
                .thenReturn(List.of(request));

        List<RequestsResponse> pending = requestsService.getAllPendingRequests();

        assertEquals(1, pending.size());
        assertEquals(State.PENDING, pending.get(0).getState());
        verify(requestsRepository).findAllByState(eq(State.PENDING), any(Pageable.class));
    }

    // -----------------------------------------------------
    // TEST: getAllOrganizationRequests()
    // -----------------------------------------------------
    @Test
    void testGetAllOrganizationRequests() {
        when(requestsRepository.findAllByOrganization_Id(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(request));

        List<RequestsResponse> orgRequests = requestsService.getAllOrganizationRequests(1L);

        assertEquals(1, orgRequests.size());
        assertEquals(organization.getName(), orgRequests.get(0).getOrganization());
        verify(requestsRepository).findAllByOrganization_Id(eq(1L), any(Pageable.class));
    }

    // -----------------------------------------------------
    // TEST: deleteOldRequests()
    // -----------------------------------------------------
    @Test
    void testDeleteOldRequests() {
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        requestsService.deleteOldRequests();

        LocalDateTime expectedCutoff = LocalDateTime.now(clock).minusDays(10);
        verify(requestsRepository).deleteOldNonPending(cutoffCaptor.capture());
        assertEquals(expectedCutoff, cutoffCaptor.getValue());
    }
}
