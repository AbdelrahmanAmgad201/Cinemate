package org.example.backend.organization;

import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieAddDTO;
import org.example.backend.movie.MovieService;
import org.example.backend.requests.RequestsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RequestsService requestsService;

    @Mock
    private MovieService movieService;

    @InjectMocks
    private OrganizationService organizationService;

    private MovieAddDTO movieAddDTO;
    private Movie savedMovie;

    @BeforeEach
    void setUp() {
        movieAddDTO = new MovieAddDTO();
        movieAddDTO.setName("Test Movie");
        movieAddDTO.setOrganizationId(1L);

        savedMovie = Movie.builder()
                .movieID(10L)
                .name("Test Movie")
                .build();
    }

    @Test
    void testRequestMovie() {

        // Mock: when movieService.addMovie() is called -> return savedMovie
        when(movieService.addMovie(movieAddDTO)).thenReturn(savedMovie);

        // Execute
        Long result = organizationService.requestMovie(movieAddDTO);

        // Assertions
        assertEquals(10L, result);

        // Verify calls
        verify(movieService, times(1)).addMovie(movieAddDTO);
        verify(requestsService, times(1)).addRequest(savedMovie);
        verifyNoMoreInteractions(movieService, requestsService);
    }
}
