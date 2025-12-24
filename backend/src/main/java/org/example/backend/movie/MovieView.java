package org.example.backend.movie;

public interface MovieView {
    Long getMovieID();
    String getName();
    String getThumbnailUrl();
    Double getAverageRating();
    Integer getDuration();
}
