package org.example.backend.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.admin.Admin;
import org.example.backend.organization.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
@ActiveProfiles("test")
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MovieService movieService;

    private Movie movie1;
    private Movie movie2;
    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = Organization.builder()
                .id(1L)
                .name("Warner Bros")
                .email("warner@example.com")
                .password("password")
                .createdAt(LocalDateTime.now())
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
                .build();
    }

    @Test
    @WithMockUser
    void testSearchMoviesPost_WithAllFilters() throws Exception {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                "dark",
                Genre.ACTION,
                "rating",
                "desc",
                0,
                10
        );

        List<Movie> movies = Arrays.asList(movie1);
        Page<Movie> moviePage = new PageImpl<>(movies, PageRequest.of(0, 10), 1);

        when(movieService.getMovies(any(MovieRequestDTO.class))).thenReturn(moviePage);

        // Act & Assert
        mockMvc.perform(post("/api/movie/v1/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("The Dark Knight"))
                .andExpect(jsonPath("$.content[0].genre").value("ACTION"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void testSearchMoviesPost_NoFilters() throws Exception {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                null,
                null,
                0,
                10
        );

        List<Movie> movies = Arrays.asList(movie1, movie2);
        Page<Movie> moviePage = new PageImpl<>(movies, PageRequest.of(0, 10), 2);

        when(movieService.getMovies(any(MovieRequestDTO.class))).thenReturn(moviePage);

        // Act & Assert
        mockMvc.perform(post("/api/movie/v1/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser
    void testSearchMoviesPost_WithGenreFilter() throws Exception {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                Genre.SCIFI,
                null,
                null,
                0,
                10
        );

        List<Movie> movies = Arrays.asList(movie2);
        Page<Movie> moviePage = new PageImpl<>(movies, PageRequest.of(0, 10), 1);

        when(movieService.getMovies(any(MovieRequestDTO.class))).thenReturn(moviePage);

        // Act & Assert
        mockMvc.perform(post("/api/movie/v1/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].genre").value("SCIFI"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void testSearchMoviesPost_WithNameFilter() throws Exception {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                "inception",
                null,
                null,
                null,
                0,
                10
        );

        List<Movie> movies = Arrays.asList(movie2);
        Page<Movie> moviePage = new PageImpl<>(movies, PageRequest.of(0, 10), 1);

        when(movieService.getMovies(any(MovieRequestDTO.class))).thenReturn(moviePage);

        // Act & Assert
        mockMvc.perform(post("/api/movie/v1/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Inception"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void testSearchMoviesPost_WithPagination() throws Exception {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                null,
                null,
                1,
                1
        );

        List<Movie> movies = Arrays.asList(movie2);
        Page<Movie> moviePage = new PageImpl<>(movies, PageRequest.of(1, 1), 2);

        when(movieService.getMovies(any(MovieRequestDTO.class))).thenReturn(moviePage);

        // Act & Assert
        mockMvc.perform(post("/api/movie/v1/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    @WithMockUser
    void testSearchMoviesPost_EmptyResult() throws Exception {
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

        when(movieService.getMovies(any(MovieRequestDTO.class))).thenReturn(moviePage);

        // Act & Assert
        mockMvc.perform(post("/api/movie/v1/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser
    void testSearchMoviesPost_WithSorting() throws Exception {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "rating",
                "desc",
                0,
                10
        );

        List<Movie> movies = Arrays.asList(movie2, movie1);
        Page<Movie> moviePage = new PageImpl<>(movies, PageRequest.of(0, 10), 2);

        when(movieService.getMovies(any(MovieRequestDTO.class))).thenReturn(moviePage);

        // Act & Assert
        mockMvc.perform(post("/api/movie/v1/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].averageRating").value(4.8))
                .andExpect(jsonPath("$.content[1].averageRating").value(4.5))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}