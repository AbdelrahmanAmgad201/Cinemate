package org.example.backend.watchHistory;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WatchHistoryResponse {
    private Long id;
    private Long movieId;
    private String movieName;
    private LocalDateTime watchedAt;

    public static WatchHistoryResponse from(WatchHistory watchHistory) {
        return WatchHistoryResponse.builder()
                .id(watchHistory.getId())
                .movieId(watchHistory.getMovieId())
                .movieName(watchHistory.getMovieName())
                .watchedAt(watchHistory.getWatchedAt())
                .build();
    }
}
