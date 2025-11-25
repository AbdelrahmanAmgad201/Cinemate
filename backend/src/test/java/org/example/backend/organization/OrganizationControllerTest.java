package org.example.backend.organization;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieAddDTO;
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
class OrganizationControllerTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private RequestsService requestsService;


    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private OrganizationController organizationController;

    private MovieAddDTO movieAddDTO;
    private OrganizationDataDTO organizationDataDTO;
    private Movie movie1;
    private Movie movie2;
    private Requests request1;
    private Requests request2;

    @BeforeEach
    void setUp() {
        // Movie DTO for addMovie
        movieAddDTO = new MovieAddDTO();
        movieAddDTO.setName("Movie 1");
        movieAddDTO.setDescription("Description 1");
        movieAddDTO.setMovieUrl("http://movie1.url");
        movieAddDTO.setThumbnailUrl("http://thumb1.url");
        movieAddDTO.setOrganizationId(1L);

        // Organization DTO for setPersonalData
        organizationDataDTO = new OrganizationDataDTO();
        organizationDataDTO.setName("Org 1");
        organizationDataDTO.setAbout("About Org 1");

        // Sample Movies
        movie1 = Movie.builder().movieID(1L).name("Movie 1").description("Desc 1").build();
        movie2 = Movie.builder().movieID(2L).name("Movie 2").description("Desc 2").build();

        // Requests containing full Movie objects
        request1 = new Requests();
        request1.setId(1L);
        request1.setMovie(movie1);

        request2 = new Requests();
        request2.setId(2L);
        request2.setMovie(movie2);
    }

    @Test
    void testGetProfile() {
        when(httpServletRequest.getAttribute("userId")).thenReturn(1L);
        when(httpServletRequest.getAttribute("userEmail")).thenReturn("org@example.com");

        ResponseEntity<?> response = organizationController.getProfile(httpServletRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("User profile for ID: 1, Email: org@example.com", response.getBody());
    }

    @Test
    void testSetPersonalData() {
        when(httpServletRequest.getAttribute("userId")).thenReturn(1L);
        when(organizationService.setOrganizationData(1L, organizationDataDTO))
                .thenReturn("Organization data saved");

        ResponseEntity<String> response = organizationController.setPersonalData(httpServletRequest, organizationDataDTO);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Organization data saved", response.getBody());
        verify(organizationService, times(1)).setOrganizationData(1L, organizationDataDTO);
    }

    @Test
    void testAddMovie() {
        when(organizationService.requestMovie(movieAddDTO)).thenReturn(100L);

        Long movieId = organizationController.addMovie(movieAddDTO);

        assertEquals(100L, movieId);
        verify(organizationService, times(1)).requestMovie(movieAddDTO);
    }

    @Test
    void testGetOrgRequests() {
        Long orgId = 1L;
        List<Requests> requests = Arrays.asList(request1, request2);

        when(requestsService.getAllOrganizationRequests(orgId)).thenReturn(requests);

        List<Requests> response = organizationController.getOrgRequests(orgId);

        assertEquals(2, response.size());
        assertEquals("Movie 1", response.get(0).getMovie().getName());
        assertEquals("Movie 2", response.get(1).getMovie().getName());
        verify(requestsService, times(1)).getAllOrganizationRequests(orgId);
    }
}
