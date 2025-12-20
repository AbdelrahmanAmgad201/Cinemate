package org.example.backend.watchHistory;


import java.time.LocalDateTime;

public interface WatchHistoryViewDTO {
    public Long getId();
    public Long getMovieId();
    public String getMovieName();
    public LocalDateTime getWatchedAt();
}
