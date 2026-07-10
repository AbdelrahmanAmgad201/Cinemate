package org.example.backend.movie;

import org.example.backend.AbstractMySQLIntegrationTest;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link MovieSpecification} against a real database instead of mocking
 * the JPA Criteria API, so a broken filter/sort actually fails these tests (the
 * previous mock-based version only checked which CriteriaBuilder methods were
 * called, and would pass even if the resulting query returned the wrong movies).
 */
@DataJpaTest
@EntityScan(basePackages = "org.example.backend")
@EnableJpaRepositories(basePackages = "org.example.backend")
class MovieSpecificationTest extends AbstractMySQLIntegrationTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Movie darkKnight;
    private Movie inception;
    private Movie batmanBegins;
    private Movie matrix;

    @BeforeEach
    void setUp() {
        Organization org = organizationRepository.save(
                Organization.builder().name("Org").email("org@test.com").password("pass").build());

        darkKnight = movieRepository.save(Movie.builder()
                .name("The Dark Knight").genre(Genre.ACTION).averageRating(9.0)
                .releaseDate(LocalDate.of(2008, 7, 18)).organization(org).build());
        inception = movieRepository.save(Movie.builder()
                .name("Inception").genre(Genre.SCIFI).averageRating(8.8)
                .releaseDate(LocalDate.of(2010, 7, 16)).organization(org).build());
        batmanBegins = movieRepository.save(Movie.builder()
                .name("Batman Begins").genre(Genre.ACTION).averageRating(8.2)
                .releaseDate(LocalDate.of(2005, 6, 15)).organization(org).build());
        matrix = movieRepository.save(Movie.builder()
                .name("The Matrix").genre(Genre.SCIFI).averageRating(8.7)
                .releaseDate(LocalDate.of(1999, 3, 31)).organization(org).build());
    }

    private Page<Movie> filter(MovieRequestDTO dto) {
        return movieRepository.findAll(MovieSpecification.filterMovies(dto), PageRequest.of(0, 10));
    }

    @Test
    void filterMovies_ByPartialCaseInsensitiveName_ReturnsOnlyMatches() {
        MovieRequestDTO dto = new MovieRequestDTO("dark", null, null, null, 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).containsExactly(darkKnight);
    }

    @Test
    void filterMovies_ByUppercaseNameQuery_StillMatchesLowercaseTitle() {
        MovieRequestDTO dto = new MovieRequestDTO("DARK KNIGHT", null, null, null, 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).containsExactly(darkKnight);
    }

    @Test
    void filterMovies_ByGenre_ReturnsOnlyThatGenreOrderedByDefaultSort() {
        MovieRequestDTO dto = new MovieRequestDTO(null, Genre.ACTION, null, null, 0, 10);

        Page<Movie> result = filter(dto);

        // Default sort is releaseDate desc: Dark Knight (2008) before Batman Begins (2005).
        assertThat(result.getContent()).containsExactly(darkKnight, batmanBegins);
    }

    @Test
    void filterMovies_ByNameAndGenreCombined_AppliesBothFilters() {
        MovieRequestDTO dto = new MovieRequestDTO("batman", Genre.ACTION, null, null, 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).containsExactly(batmanBegins);
    }

    @Test
    void filterMovies_ByNameMatchingWrongGenre_ReturnsEmpty() {
        MovieRequestDTO dto = new MovieRequestDTO("batman", Genre.SCIFI, null, null, 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void filterMovies_SortByRatingAscending_OrdersLowestFirst() {
        MovieRequestDTO dto = new MovieRequestDTO(null, null, "rating", "asc", 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).containsExactly(batmanBegins, matrix, inception, darkKnight);
    }

    @Test
    void filterMovies_SortByRatingDescending_OrdersHighestFirst() {
        MovieRequestDTO dto = new MovieRequestDTO(null, null, "rating", "desc", 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).containsExactly(darkKnight, inception, matrix, batmanBegins);
    }

    @Test
    void filterMovies_SortByReleaseDateAscending_OldestFirst() {
        MovieRequestDTO dto = new MovieRequestDTO(null, null, "releaseDate", "asc", 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).containsExactly(matrix, batmanBegins, darkKnight, inception);
    }

    @Test
    void filterMovies_SortByReleaseDateUnderscoreAlias_NewestFirst() {
        MovieRequestDTO dto = new MovieRequestDTO(null, null, "release_date", "desc", 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).containsExactly(inception, darkKnight, batmanBegins, matrix);
    }

    @Test
    void filterMovies_SortByNameAscending_IsAlphabetical() {
        MovieRequestDTO dto = new MovieRequestDTO(null, null, "name", "asc", 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent())
                .extracting(Movie::getName)
                .containsExactly("Batman Begins", "Inception", "The Dark Knight", "The Matrix");
    }

    @Test
    void filterMovies_DefaultSort_IsReleaseDateDescending() {
        MovieRequestDTO dto = new MovieRequestDTO(null, null, null, null, 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).containsExactly(inception, darkKnight, batmanBegins, matrix);
    }

    @Test
    void filterMovies_InvalidSortKey_FallsBackToDefaultSort() {
        MovieRequestDTO dto = new MovieRequestDTO(null, null, "invalidSort", "asc", 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).containsExactly(inception, darkKnight, batmanBegins, matrix);
    }

    @Test
    void filterMovies_BlankNameString_AppliesNoNameFilter() {
        MovieRequestDTO dto = new MovieRequestDTO("   ", null, null, null, 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).hasSize(4);
    }

    @Test
    void filterMovies_BlankSortByString_FallsBackToDefaultSort() {
        MovieRequestDTO dto = new MovieRequestDTO(null, null, "   ", "asc", 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).containsExactly(inception, darkKnight, batmanBegins, matrix);
    }

    @Test
    void filterMovies_NameGenreAndSortCombined() {
        MovieRequestDTO dto = new MovieRequestDTO(null, Genre.SCIFI, "rating", "asc", 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).containsExactly(matrix, inception);
    }

    @Test
    void filterMovies_NoNameMatch_ReturnsEmptyPage() {
        MovieRequestDTO dto = new MovieRequestDTO("nonexistent title", null, null, null, 0, 10);

        Page<Movie> result = filter(dto);

        assertThat(result.getContent()).isEmpty();
    }
}
