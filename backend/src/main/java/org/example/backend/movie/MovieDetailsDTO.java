package org.example.backend.movie;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Single-movie detail view (CQ-NEW-03) — returned instead of the {@link Movie} entity
 * itself, which can't be serialized directly (CQ-NEW-02: {@code organization} has both
 * a field and a same-named {@code @JsonProperty} getter).
 */
@Data
@Builder
public class MovieDetailsDTO {
    private Long movieID;
    private String name;
    private String description;
    private String movieUrl;
    private String thumbnailUrl;
    private String trailerUrl;
    private Integer duration;
    private Genre genre;
    private LocalDate releaseDate;
    private Double averageRating;
    private String organizationName;

    public static MovieDetailsDTO from(Movie movie) {
        return MovieDetailsDTO.builder()
                .movieID(movie.getMovieID())
                .name(movie.getName())
                .description(movie.getDescription())
                .movieUrl(movie.getMovieUrl())
                .thumbnailUrl(movie.getThumbnailUrl())
                .trailerUrl(movie.getTrailerUrl())
                .duration(movie.getDuration())
                .genre(movie.getGenre())
                .releaseDate(movie.getReleaseDate())
                .averageRating(movie.getAverageRating())
                .organizationName(movie.getOrganizationName())
                .build();
    }
}
