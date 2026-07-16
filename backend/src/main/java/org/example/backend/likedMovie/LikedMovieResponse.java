package org.example.backend.likedMovie;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LikedMovieResponse {
    private Long movieId;
    private String movieName;
    private LocalDateTime dateLiked;

    public static LikedMovieResponse from(LikedMovie likedMovie) {
        return LikedMovieResponse.builder()
                .movieId(likedMovie.getLikedMoviesID().getMovieId())
                .movieName(likedMovie.getMovieName())
                .dateLiked(likedMovie.getDateLiked())
                .build();
    }
}
