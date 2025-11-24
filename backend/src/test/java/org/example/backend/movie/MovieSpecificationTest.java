package org.example.backend.movie;

import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieSpecificationTest {

    @Mock
    private Root<Movie> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Path<String> namePath;

    @Mock
    private Path<Genre> genrePath;

    @Mock
    private Path<Double> ratingPath;

    @Mock
    private Path<Object> releaseDatePath;

    @Mock
    private Predicate predicate;

    @Mock
    private Expression<String> lowerExpression;

    @Mock
    private Order ascOrder;

    @Mock
    private Order descOrder;

    @BeforeEach
    void setUp() {
        lenient().when(root.get("name")).thenReturn((Path) namePath);
        lenient().when(root.get("genre")).thenReturn((Path) genrePath);
        lenient().when(root.get("averageRating")).thenReturn((Path) ratingPath);
        lenient().when(root.get("releaseDate")).thenReturn(releaseDatePath);

        lenient().when(criteriaBuilder.lower(any())).thenReturn(lowerExpression);
        lenient().when(criteriaBuilder.like(any(), anyString())).thenReturn(predicate);
        lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(predicate);

        lenient().when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);
        lenient().when(criteriaBuilder.asc(any())).thenReturn(ascOrder);
        lenient().when(criteriaBuilder.desc(any())).thenReturn(descOrder);
    }


    @Test
    void testFilterMovies_WithNameFilter() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                "dark knight",
                null,
                null,
                null,
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).lower(namePath);
        verify(criteriaBuilder).like(lowerExpression, "%dark knight%");
        verify(query).orderBy(descOrder);
    }

    @Test
    void testFilterMovies_WithGenreFilter() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                Genre.ACTION,
                null,
                null,
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).equal(genrePath, Genre.ACTION);
        verify(query).orderBy(descOrder);
    }

    @Test
    void testFilterMovies_WithNameAndGenreFilters() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                "dark",
                Genre.ACTION,
                null,
                null,
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).lower(namePath);
        verify(criteriaBuilder).like(lowerExpression, "%dark%");
        verify(criteriaBuilder).equal(genrePath, Genre.ACTION);
        verify(query).orderBy(descOrder);
    }

    @Test
    void testFilterMovies_SortByRatingAscending() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "rating",
                "asc",
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).asc(ratingPath);
        verify(query).orderBy(ascOrder);
    }

    @Test
    void testFilterMovies_SortByRatingDescending() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "rating",
                "desc",
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).desc(ratingPath);
        verify(query).orderBy(descOrder);
    }

    @Test
    void testFilterMovies_SortByReleaseDateAscending() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "releaseDate",
                "asc",
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).asc(releaseDatePath);
        verify(query).orderBy(ascOrder);
    }

    @Test
    void testFilterMovies_SortByReleaseDateDescending() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "release_date",
                "desc",
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).desc(releaseDatePath);
        verify(query).orderBy(descOrder);
    }

    @Test
    void testFilterMovies_SortByNameAscending() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "name",
                "asc",
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).asc(namePath);
        verify(query).orderBy(ascOrder);
    }

    @Test
    void testFilterMovies_SortByNameDescending() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "name",
                "desc",
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).desc(namePath);
        verify(query).orderBy(descOrder);
    }

    @Test
    void testFilterMovies_DefaultSortWhenNoSortProvided() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                null,
                null,
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).desc(releaseDatePath);
        verify(query).orderBy(descOrder);
    }

    @Test
    void testFilterMovies_DefaultSortWhenInvalidSortProvided() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "invalidSort",
                "asc",
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).desc(releaseDatePath);
        verify(query).orderBy(descOrder);
    }

    @Test
    void testFilterMovies_WithEmptyNameString() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                "   ",
                null,
                null,
                null,
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder, never()).like(any(), anyString());
        verify(query).orderBy(descOrder);
    }

    @Test
    void testFilterMovies_WithEmptySortByString() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                null,
                null,
                "   ",
                "asc",
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).desc(releaseDatePath);
        verify(query).orderBy(descOrder);
    }

    @Test
    void testFilterMovies_AllFiltersAndSorting() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                "inception",
                Genre.SCIFI,
                "rating",
                "desc",
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).lower(namePath);
        verify(criteriaBuilder).like(lowerExpression, "%inception%");
        verify(criteriaBuilder).equal(genrePath, Genre.SCIFI);
        verify(criteriaBuilder).desc(ratingPath);
        verify(query).orderBy(descOrder);
    }

    @Test
    void testFilterMovies_CaseInsensitiveNameSearch() {
        // Arrange
        MovieRequestDTO requestDTO = new MovieRequestDTO(
                "DARK KNIGHT",
                null,
                null,
                null,
                0,
                10
        );

        // Act
        Specification<Movie> specification = MovieSpecification.filterMovies(requestDTO);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert
        assertNotNull(result);
        verify(criteriaBuilder).lower(namePath);
        verify(criteriaBuilder).like(lowerExpression, "%dark knight%");
    }
}