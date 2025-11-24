package org.example.backend.watchHistory;

import jakarta.transaction.Transactional;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WatchHistoryService {
    @Autowired
    WatchHistoryRepository watchHistoryRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MovieRepository movieRepository;

    @Transactional
    public WatchHistory addToWatchHistory(Long userId, Long movieID){
        Movie movie = movieRepository.findById(movieID)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        WatchHistory watchHistory = new WatchHistory();
        watchHistory.setUser(user);
        watchHistory.setMovie(movie);
        return  watchHistoryRepository.save(watchHistory);
    }
}

