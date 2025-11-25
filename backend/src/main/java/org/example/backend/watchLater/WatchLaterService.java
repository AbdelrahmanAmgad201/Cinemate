package org.example.backend.watchLater;


import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.example.backend.likedMovie.LikedMovieRepository;
import org.example.backend.likedMovie.LikedMoviesID;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class WatchLaterService {
    private final WatchLaterRepository watchLaterRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;


    @Transactional
    public WatchLater addMovie(Long userId, Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        WatchLaterID id = new WatchLaterID(userId, movieId);

        Optional<WatchLater> watchLater = watchLaterRepository.findById(id);
        if(watchLater.isPresent()) {
            return watchLater.get();
        }
        WatchLater newWatchLater = WatchLater.builder().
                watchLaterID(id).
                movie(movie).
                user(user).
                build();
        return watchLaterRepository.save(newWatchLater);

    }
}
