package org.example.backend.watchLater;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WatchLaterResponse {
    private Long movieId;
    private String movieName;
    private LocalDateTime dateAdded;

    public static WatchLaterResponse from(WatchLater watchLater) {
        return WatchLaterResponse.builder()
                .movieId(watchLater.getWatchLaterID().getMovieId())
                .movieName(watchLater.getMovieName())
                .dateAdded(watchLater.getDateAdded())
                .build();
    }
}
