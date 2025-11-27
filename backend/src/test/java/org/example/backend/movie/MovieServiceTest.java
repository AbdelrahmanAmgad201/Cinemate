package org.example.backend.movie;

import org.example.backend.admin.Admin;
import org.example.backend.organization.MoviesOverview;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
import org.example.backend.requests.RequestsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RequestsRepository requestsRepository;

    @InjectMocks
    private MovieService movieService;

    private Organization organization;
    private Movie movie;
    private MovieAddDTO movieAddDTO;
    private Admin admin;

    @BeforeEach
    void setup() {
        organization = Organization.builder()
                .id(1L)
                .email("org@test.com")
                .name("Test Org")
                .build();
        admin = admin.builder()
                .id(1L)
                .email("admin@test.com")
                .build();
        movie = Movie.builder()
                .movieID(10L)
                .name("Test Movie")
                .genre(Genre.ACTION)
                .organization(organization)
                .admin(admin)
                .build();

        movieAddDTO = MovieAddDTO.builder()
                .name("Test Movie")
                .description("desc")
                .thumbnailUrl("thumb")
                .movieUrl("video")
                .trailerUrl("trailer")
                .duration(100)
                .genre(Genre.ACTION)
                .build();
    }

    // -------------------------------------------------------------------
    // TEST getMovies()
    // -------------------------------------------------------------------
    @Test
    void testGetMovies() {
        MovieRequestDTO req = new MovieRequestDTO();
        req.setPage(0);
        req.setPageSize(5);

        List<Movie> movieList = List.of(movie);
        Page<Movie> moviePage = new PageImpl<>(movieList);

        // Mock the correct repository method
        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        Page<Movie> result = movieService.getMovies(req);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(movieRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
    }

    // -------------------------------------------------------------------
    // TEST addMovie()
    // -------------------------------------------------------------------
    @Test
    void testAddMovie() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);

        Movie result = movieService.addMovie(1L, movieAddDTO);

        assertNotNull(result);
        assertEquals("Test Movie", result.getName());
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void testAddMovie_OrgNotFound() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> movieService.addMovie(1L, movieAddDTO));
    }

    // -------------------------------------------------------------------
    // TEST getMovie()
    // -------------------------------------------------------------------
    @Test
    void testGetMovie() {
        when(movieRepository.findById(10L)).thenReturn(Optional.of(movie));

        Movie result = movieService.getMovie(10L);

        assertEquals(10L, result.getMovieID());
        verify(movieRepository).findById(10L);
    }

    @Test
    void testGetMovie_NotFound() {
        when(movieRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> movieService.getMovie(10L));
    }

    // -------------------------------------------------------------------
    // TEST getMoviesOverview()
    // -------------------------------------------------------------------
    @Test
    void testGetMoviesOverview() {
        // 5 movies, 100 views
        Object[] countViewObj = new Object[]{5L, 100L};

        // Most popular genres (List<Object[]>)
        List<Object[]> genres = new ArrayList<>();
        genres.add(new Object[]{Genre.ACTION, 50L});

        when(movieRepository.getMovieCountAndTotalViews(1L))
                .thenReturn(countViewObj);

        when(movieRepository.getGenresOrderedByViews(1L))
                .thenReturn(genres);

        MoviesOverview overview = movieService.getMoviesOverview(1L);

        assertEquals(5L, overview.getNumberOfMovies());
        assertEquals(100L, overview.getTotalViewsAcrossAllMovies());
        assertEquals(Genre.ACTION, overview.getMostPopularGenre());
    }




    @Test
    void testGetMoviesOverview_NoGenres() {
        Object[] countViewsObj = new Object[]{3L, 20L};

        when(movieRepository.getMovieCountAndTotalViews(1L)).thenReturn(countViewsObj);
        when(movieRepository.getGenresOrderedByViews(1L)).thenReturn(List.of());

        MoviesOverview overview = movieService.getMoviesOverview(1L);

        assertNull(overview.getMostPopularGenre());
    }

    // -------------------------------------------------------------------
    // TEST getOrganizationMovies()
    // -------------------------------------------------------------------
    @Test
    void testGetOrganizationMovies() {
        when(movieRepository.findByAdminIsNotNullAndOrganization_Id(1L))
                .thenReturn(List.of(movie));

        List<Movie> result = movieService.getOrganizationMovies(1L);

        assertEquals(1, result.size());
        verify(movieRepository).findByAdminIsNotNullAndOrganization_Id(1L);
    }

    // -------------------------------------------------------------------
    // TEST getMovieStatsByMovieId()
    // -------------------------------------------------------------------
    @Test
    void testGetMovieStatsByMovieId() {
        OneMovieOverView overview = new OneMovieOverView(
                100L, 50L, 30L, 30L, 4.5, 20L
        );

        when(movieRepository.getMovieOverview(10L)).thenReturn(overview);

        OneMovieOverView result = movieService.getMovieStatsByMovieId(10L);

        assertEquals(100, result.getViews());
        assertEquals(20, result.getNumberOfWatchLaters());
    }

    // -------------------------------------------------------------------
    // TEST OrganizationOwnMovie()
    // -------------------------------------------------------------------
    @Test
    void testOrganizationOwnMovie_True() {
        when(movieRepository.findById(10L)).thenReturn(Optional.of(movie));

        boolean result = movieService.OrganizationOwnMovie(1L, 10L);

        assertTrue(result);
    }

    @Test
    void testOrganizationOwnMovie_False() {
        Organization otherOrg = Organization.builder().id(2L).build();
        Movie otherMovie = Movie.builder().movieID(10L).organization(otherOrg).build();

        when(movieRepository.findById(10L)).thenReturn(Optional.of(otherMovie));

        assertFalse(movieService.OrganizationOwnMovie(1L, 10L));
    }

    @Test
    void testOrganizationOwnMovie_NotFound() {
        when(movieRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> movieService.OrganizationOwnMovie(1L, 10L));
    }

}
