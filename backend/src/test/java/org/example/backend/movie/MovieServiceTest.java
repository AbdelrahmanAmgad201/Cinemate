package org.example.backend.movie;

import org.example.backend.admin.Admin;
import org.example.backend.organization.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    private Movie movie1;
    private Movie movie2;
    private Movie movie3;
    private Organization organization;
    private Admin admin;

    @BeforeEach
    void setUp() {
        organization = Organization.builder()
                .id(1L)
                .name("Warner Bros")
                .email("warner@example.com")
                .password("password")
                .createdAt(LocalDateTime.now())
                .build();

        admin = Admin.builder()
                .id(1L)
                .name("Admin User")
                .email("admin@example.com")
                .password("password")
                .build();

        movie1 = Movie.builder()
                .movieID(1L)
                .name("The Dark Knight")
                .description("Batman fights the Joker")
                .genre(Genre.ACTION)
                .releaseDate(LocalDate.of(2008, 7, 18))
                .duration(152)
                .ratingSum(450L)
                .ratingCount(100)
                .averageRating(4.5)
                .organization(organization)
                .admin(admin)
                .build();

        movie2 = Movie.builder()
                .movieID(2L)
                .name("Inception")
                .description("Dream within a dream")
                .genre(Genre.SCIFI)
                .releaseDate(LocalDate.of(2010, 7, 16))
                .duration(148)
                .ratingSum(480L)
                .ratingCount(100)
                .averageRating(4.8)
                .organization(organization)
                .admin(admin)
                .build();

        movie3 = Movie.builder()
                .movieID(3L)
                .name("The Conjuring")
                .description("Horror story")
                .genre(Genre.HORROR)
                .releaseDate(LocalDate.of(2013, 7, 19))
                .duration(112)
                .ratingSum(400L)
                .ratingCount(100)
                .averageRating(4.0)
                .organization(organization)
                .admin(admin)
                .build();
    }

    @Test
    void testGetMovies_WithAllFilters() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                "dark",
                Genre.ACTION,
                "rating",
                "desc",
                0,
                10
        );

        List<Movie> movieList = Arrays.asList(movie1);
        Page<Movie> moviePage = new PageImpl<>(movieList, PageRequest.of(0, 10), movieList.size());

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        Page<Movie> result = movieService.getMovies(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("The Dark Knight", result.getContent().get(0).getName());
        verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetMovies_WithNameFilterOnly() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                "inception",
                null,
                null,
                null,
                0,
                10
        );

        List<Movie> movieList = Arrays.asList(movie2);
        Page<Movie> moviePage = new PageImpl<>(movieList, PageRequest.of(0, 10), movieList.size());

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        Page<Movie> result = movieService.getMovies(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Inception", result.getContent().get(0).getName());
        verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetMovies_WithGenreFilterOnly() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                Genre.HORROR,
                null,
                null,
                0,
                10
        );

        List<Movie> movieList = Arrays.asList(movie3);
        Page<Movie> moviePage = new PageImpl<>(movieList, PageRequest.of(0, 10), movieList.size());

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        Page<Movie> result = movieService.getMovies(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(Genre.HORROR, result.getContent().get(0).getGenre());
        verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetMovies_NoFilters() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                null,
                null,
                0,
                10
        );

        List<Movie> movieList = Arrays.asList(movie1, movie2, movie3);
        Page<Movie> moviePage = new PageImpl<>(movieList, PageRequest.of(0, 10), movieList.size());

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        Page<Movie> result = movieService.getMovies(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetMovies_WithSortByRatingAscending() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "rating",
                "asc",
                0,
                10
        );

        List<Movie> movieList = Arrays.asList(movie3, movie1, movie2);
        Page<Movie> moviePage = new PageImpl<>(movieList, PageRequest.of(0, 10), movieList.size());

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        Page<Movie> result = movieService.getMovies(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetMovies_WithSortByReleaseDateDescending() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "releaseDate",
                "desc",
                0,
                10
        );

        List<Movie> movieList = Arrays.asList(movie3, movie2, movie1);
        Page<Movie> moviePage = new PageImpl<>(movieList, PageRequest.of(0, 10), movieList.size());

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        Page<Movie> result = movieService.getMovies(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetMovies_WithPagination() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                null,
                null,
                1,
                2
        );

        List<Movie> movieList = Arrays.asList(movie3);
        Page<Movie> moviePage = new PageImpl<>(movieList, PageRequest.of(1, 2), 3);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        Page<Movie> result = movieService.getMovies(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getNumber());
        verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetMovies_EmptyResult() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                "nonexistent",
                null,
                null,
                null,
                0,
                10
        );

        Page<Movie> moviePage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        Page<Movie> result = movieService.getMovies(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetMovies_WithEmptyNameString() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                "   ",
                null,
                null,
                null,
                0,
                10
        );

        List<Movie> movieList = Arrays.asList(movie1, movie2, movie3);
        Page<Movie> moviePage = new PageImpl<>(movieList, PageRequest.of(0, 10), movieList.size());

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        Page<Movie> result = movieService.getMovies(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetMovies_WithSortByName() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "name",
                "asc",
                0,
                10
        );

        List<Movie> movieList = Arrays.asList(movie2, movie1, movie3);
        Page<Movie> moviePage = new PageImpl<>(movieList, PageRequest.of(0, 10), movieList.size());

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        Page<Movie> result = movieService.getMovies(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }
}