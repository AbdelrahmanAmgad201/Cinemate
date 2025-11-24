package org.example.backend.movie;

import org.example.backend.admin.Admin;
import org.example.backend.organization.Organization;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MovieEntityTest {

    @Test
    void testMovieBuilder() {
        // Arrange & Act
        Organization organization = Organization.builder()
                .id(1L)
                .name("Warner Bros")
                .email("warner@example.com")
                .password("password")
                .createdAt(LocalDateTime.now())
                .build();

        Admin admin = Admin.builder()
                .id(1L)
                .name("Admin")
                .email("admin@example.com")
                .password("password")
                .build();

        Movie movie = Movie.builder()
                .movieID(1L)
                .name("The Dark Knight")
                .description("Batman fights the Joker")
                .movieUrl("http://example.com/movie")
                .thumbnailUrl("http://example.com/thumbnail")
                .trailerUrl("http://example.com/trailer")
                .duration(152)
                .genre(Genre.ACTION)
                .releaseDate(LocalDate.of(2008, 7, 18))
                .ratingSum(450L)
                .ratingCount(100)
                .averageRating(4.5)
                .organization(organization)
                .admin(admin)
                .build();

        // Assert
        assertNotNull(movie);
        assertEquals(1L, movie.getMovieID());
        assertEquals("The Dark Knight", movie.getName());
        assertEquals("Batman fights the Joker", movie.getDescription());
        assertEquals("http://example.com/movie", movie.getMovieUrl());
        assertEquals("http://example.com/thumbnail", movie.getThumbnailUrl());
        assertEquals("http://example.com/trailer", movie.getTrailerUrl());
        assertEquals(152, movie.getDuration());
        assertEquals(Genre.ACTION, movie.getGenre());
        assertEquals(LocalDate.of(2008, 7, 18), movie.getReleaseDate());
        assertEquals(450L, movie.getRatingSum());
        assertEquals(100, movie.getRatingCount());
        assertEquals(4.5, movie.getAverageRating());
        assertEquals(organization, movie.getOrganization());
        assertEquals(admin, movie.getAdmin());
    }

    @Test
    void testMovieSettersAndGetters() {
        // Arrange
        Movie movie = new Movie();
        Organization organization = Organization.builder()
                .id(1L)
                .name("Universal")
                .build();

        // Act
        movie.setMovieID(2L);
        movie.setName("Inception");
        movie.setDescription("Dream within a dream");
        movie.setMovieUrl("http://example.com/inception");
        movie.setThumbnailUrl("http://example.com/inception-thumb");
        movie.setTrailerUrl("http://example.com/inception-trailer");
        movie.setDuration(148);
        movie.setGenre(Genre.SCIFI);
        movie.setReleaseDate(LocalDate.of(2010, 7, 16));
        movie.setRatingSum(480L);
        movie.setRatingCount(100);
        movie.setAverageRating(4.8);
        movie.setOrganization(organization);

        // Assert
        assertEquals(2L, movie.getMovieID());
        assertEquals("Inception", movie.getName());
        assertEquals("Dream within a dream", movie.getDescription());
        assertEquals("http://example.com/inception", movie.getMovieUrl());
        assertEquals("http://example.com/inception-thumb", movie.getThumbnailUrl());
        assertEquals("http://example.com/inception-trailer", movie.getTrailerUrl());
        assertEquals(148, movie.getDuration());
        assertEquals(Genre.SCIFI, movie.getGenre());
        assertEquals(LocalDate.of(2010, 7, 16), movie.getReleaseDate());
        assertEquals(480L, movie.getRatingSum());
        assertEquals(100, movie.getRatingCount());
        assertEquals(4.8, movie.getAverageRating());
        assertEquals(organization, movie.getOrganization());
    }

    @Test
    void testMovieNoArgsConstructor() {
        // Act
        Movie movie = new Movie();

        // Assert
        assertNotNull(movie);
        assertNull(movie.getMovieID());
        assertNull(movie.getName());
        assertNull(movie.getDescription());
        assertNull(movie.getGenre());
        assertNull(movie.getOrganization());
    }

    @Test
    void testMovieAllArgsConstructor() {
        // Arrange
        Organization organization = Organization.builder()
                .id(1L)
                .name("Paramount")
                .build();
        Admin admin = Admin.builder()
                .id(1L)
                .name("Admin")
                .build();

        // Act
        Movie movie = new Movie(
                1L,
                "Interstellar",
                "Space adventure",
                "http://example.com/interstellar",
                "http://example.com/interstellar-thumb",
                "http://example.com/interstellar-trailer",
                169,
                Genre.SCIFI,
                LocalDate.of(2014, 11, 7),
                490L,
                100,
                4.9,
                organization,
                admin
        );

        // Assert
        assertEquals(1L, movie.getMovieID());
        assertEquals("Interstellar", movie.getName());
        assertEquals("Space adventure", movie.getDescription());
        assertEquals(Genre.SCIFI, movie.getGenre());
        assertEquals(169, movie.getDuration());
        assertEquals(organization, movie.getOrganization());
        assertEquals(admin, movie.getAdmin());
    }

    @Test
    void testMovieWithNullableFields() {
        // Act
        Movie movie = Movie.builder()
                .movieID(1L)
                .name("Test Movie")
                .build();

        // Assert
        assertNotNull(movie);
        assertEquals(1L, movie.getMovieID());
        assertEquals("Test Movie", movie.getName());
        assertNull(movie.getDescription());
        assertNull(movie.getMovieUrl());
        assertNull(movie.getThumbnailUrl());
        assertNull(movie.getTrailerUrl());
        assertNull(movie.getDuration());
        assertNull(movie.getGenre());
        assertNull(movie.getReleaseDate());
        assertNull(movie.getRatingSum());
        assertNull(movie.getRatingCount());
        assertNull(movie.getAverageRating());
        assertNull(movie.getOrganization());
        assertNull(movie.getAdmin());
    }

    @Test
    void testMovieRatingCalculation() {
        // Arrange
        Movie movie = Movie.builder()
                .ratingSum(450L)
                .ratingCount(100)
                .averageRating(4.5)
                .build();

        // Assert
        assertEquals(450L, movie.getRatingSum());
        assertEquals(100, movie.getRatingCount());
        assertEquals(4.5, movie.getAverageRating());

        // Verify the average matches the calculation
        double expectedAverage = (double) movie.getRatingSum() / movie.getRatingCount();
        assertEquals(expectedAverage, movie.getAverageRating(), 0.01);
    }

    @Test
    void testGenreEnum() {
        // Act & Assert
        assertEquals(Genre.ACTION, Genre.valueOf("ACTION"));
        assertEquals(Genre.COMEDY, Genre.valueOf("COMEDY"));
        assertEquals(Genre.DRAMA, Genre.valueOf("DRAMA"));
        assertEquals(Genre.HORROR, Genre.valueOf("HORROR"));
        assertEquals(Genre.SCIFI, Genre.valueOf("SCIFI"));
        assertEquals(Genre.THRILLER, Genre.valueOf("THRILLER"));
        assertEquals(Genre.ROMANCE, Genre.valueOf("ROMANCE"));
        assertEquals(Genre.DOCUMENTARY, Genre.valueOf("DOCUMENTARY"));
        assertEquals(Genre.ANIMATION, Genre.valueOf("ANIMATION"));
        assertEquals(Genre.MYSTERY, Genre.valueOf("MYSTERY"));
    }

    @Test
    void testAllGenresAvailable() {
        // Act
        Genre[] genres = Genre.values();

        // Assert
        assertEquals(10, genres.length);
        assertTrue(java.util.Arrays.asList(genres).contains(Genre.ACTION));
        assertTrue(java.util.Arrays.asList(genres).contains(Genre.COMEDY));
        assertTrue(java.util.Arrays.asList(genres).contains(Genre.DRAMA));
        assertTrue(java.util.Arrays.asList(genres).contains(Genre.HORROR));
        assertTrue(java.util.Arrays.asList(genres).contains(Genre.SCIFI));
        assertTrue(java.util.Arrays.asList(genres).contains(Genre.THRILLER));
        assertTrue(java.util.Arrays.asList(genres).contains(Genre.ROMANCE));
        assertTrue(java.util.Arrays.asList(genres).contains(Genre.DOCUMENTARY));
        assertTrue(java.util.Arrays.asList(genres).contains(Genre.ANIMATION));
        assertTrue(java.util.Arrays.asList(genres).contains(Genre.MYSTERY));
    }
}