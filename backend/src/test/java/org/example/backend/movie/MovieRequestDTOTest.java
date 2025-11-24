package org.example.backend.movie;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovieRequestDTOTest {

    @Test
    void testNoArgsConstructor() {
        // Act
        MovieRequestDTO dto = new MovieRequestDTO();

        // Assert
        assertNotNull(dto);
        assertNull(dto.getName());
        assertNull(dto.getGenre());
        assertNull(dto.getSortBy());
        assertNull(dto.getSortDirection());
        assertEquals(0, dto.getPage());
        assertEquals(0, dto.getPageSize());
    }

    @Test
    void testAllArgsConstructor() {
        // Act
        MovieRequestDTO dto = new MovieRequestDTO(
                "Inception",
                Genre.SCIFI,
                "rating",
                "desc",
                0,
                10
        );

        // Assert
        assertEquals("Inception", dto.getName());
        assertEquals(Genre.SCIFI, dto.getGenre());
        assertEquals("rating", dto.getSortBy());
        assertEquals("desc", dto.getSortDirection());
        assertEquals(0, dto.getPage());
        assertEquals(10, dto.getPageSize());
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        MovieRequestDTO dto = new MovieRequestDTO();

        // Act
        dto.setName("The Dark Knight");
        dto.setGenre(Genre.ACTION);
        dto.setSortBy("releaseDate");
        dto.setSortDirection("asc");
        dto.setPage(1);
        dto.setPageSize(20);

        // Assert
        assertEquals("The Dark Knight", dto.getName());
        assertEquals(Genre.ACTION, dto.getGenre());
        assertEquals("releaseDate", dto.getSortBy());
        assertEquals("asc", dto.getSortDirection());
        assertEquals(1, dto.getPage());
        assertEquals(20, dto.getPageSize());
    }

    @Test
    void testWithNullValues() {
        // Act
        MovieRequestDTO dto = new MovieRequestDTO(
                null,
                null,
                null,
                null,
                0,
                10
        );

        // Assert
        assertNull(dto.getName());
        assertNull(dto.getGenre());
        assertNull(dto.getSortBy());
        assertNull(dto.getSortDirection());
        assertEquals(0, dto.getPage());
        assertEquals(10, dto.getPageSize());
    }

    @Test
    void testWithEmptyStrings() {
        // Act
        MovieRequestDTO dto = new MovieRequestDTO(
                "",
                null,
                "",
                "",
                0,
                10
        );

        // Assert
        assertEquals("", dto.getName());
        assertNull(dto.getGenre());
        assertEquals("", dto.getSortBy());
        assertEquals("", dto.getSortDirection());
    }

    @Test
    void testWithWhitespaceStrings() {
        // Act
        MovieRequestDTO dto = new MovieRequestDTO(
                "   ",
                null,
                "   ",
                "   ",
                0,
                10
        );

        // Assert
        assertEquals("   ", dto.getName());
        assertEquals("   ", dto.getSortBy());
        assertEquals("   ", dto.getSortDirection());
    }

    @Test
    void testWithAllGenres() {
        // Test with each genre
        for (Genre genre : Genre.values()) {
            MovieRequestDTO dto = new MovieRequestDTO(
                    null,
                    genre,
                    null,
                    null,
                    0,
                    10
            );

            assertEquals(genre, dto.getGenre());
        }
    }

    @Test
    void testWithDifferentSortOptions() {
        // Test rating sort
        MovieRequestDTO dto1 = new MovieRequestDTO(null, null, "rating", "desc", 0, 10);
        assertEquals("rating", dto1.getSortBy());
        assertEquals("desc", dto1.getSortDirection());

        // Test releaseDate sort
        MovieRequestDTO dto2 = new MovieRequestDTO(null, null, "releaseDate", "asc", 0, 10);
        assertEquals("releaseDate", dto2.getSortBy());
        assertEquals("asc", dto2.getSortDirection());

        // Test name sort
        MovieRequestDTO dto3 = new MovieRequestDTO(null, null, "name", "asc", 0, 10);
        assertEquals("name", dto3.getSortBy());
        assertEquals("asc", dto3.getSortDirection());
    }

    @Test
    void testWithDifferentPaginationValues() {
        // Test first page
        MovieRequestDTO dto1 = new MovieRequestDTO(null, null, null, null, 0, 10);
        assertEquals(0, dto1.getPage());
        assertEquals(10, dto1.getPageSize());

        // Test second page
        MovieRequestDTO dto2 = new MovieRequestDTO(null, null, null, null, 1, 20);
        assertEquals(1, dto2.getPage());
        assertEquals(20, dto2.getPageSize());

        // Test large page size
        MovieRequestDTO dto3 = new MovieRequestDTO(null, null, null, null, 5, 100);
        assertEquals(5, dto3.getPage());
        assertEquals(100, dto3.getPageSize());
    }

    @Test
    void testCompleteRequest() {
        // Act
        MovieRequestDTO dto = new MovieRequestDTO(
                "Batman",
                Genre.ACTION,
                "rating",
                "desc",
                2,
                15
        );

        // Assert
        assertEquals("Batman", dto.getName());
        assertEquals(Genre.ACTION, dto.getGenre());
        assertEquals("rating", dto.getSortBy());
        assertEquals("desc", dto.getSortDirection());
        assertEquals(2, dto.getPage());
        assertEquals(15, dto.getPageSize());
    }

    @Test
    void testMinimalRequest() {
        // Act
        MovieRequestDTO dto = new MovieRequestDTO(
                null,
                null,
                null,
                null,
                0,
                10
        );

        // Assert
        assertNull(dto.getName());
        assertNull(dto.getGenre());
        assertNull(dto.getSortBy());
        assertNull(dto.getSortDirection());
        assertEquals(0, dto.getPage());
        assertEquals(10, dto.getPageSize());
    }

    @Test
    void testCaseInsensitiveNameHandling() {
        // Act
        MovieRequestDTO dto1 = new MovieRequestDTO("INCEPTION", null, null, null, 0, 10);
        MovieRequestDTO dto2 = new MovieRequestDTO("inception", null, null, null, 0, 10);
        MovieRequestDTO dto3 = new MovieRequestDTO("InCePtIoN", null, null, null, 0, 10);

        // Assert - DTOs store the name as-is (case sensitivity is handled in specification)
        assertEquals("INCEPTION", dto1.getName());
        assertEquals("inception", dto2.getName());
        assertEquals("InCePtIoN", dto3.getName());
    }
}