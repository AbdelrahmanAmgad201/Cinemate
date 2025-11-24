package org.example.backend.likedMovie;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.movieReview.MovieReviewID;
import org.example.backend.movieReview.MovieReviewRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikedMovieService {
    private final LikedMovieRepository likedMovieRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Transactional
    public LikedMovie likeMovie(Long userId, Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LikedMoviesID id = new LikedMoviesID(userId, movieId);

        Optional<LikedMovie> existingLike = likedMovieRepository.findById(id);
        if (existingLike.isPresent()) {
            return existingLike.get();
        }

        LikedMovie likedMovie = LikedMovie.builder()
                .likedMoviesID(id)
                .user(user)
                .movie(movie)
                .build();

        return likedMovieRepository.save(likedMovie);
    }

}
