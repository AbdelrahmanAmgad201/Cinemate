package org.example.backend.movieReview;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.PrivateProfileException;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieReviewService {

    private final MovieReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Transactional
    public MovieReview addOrUpdateReview(Long userId, MovieReviewDTO dto) {

        Movie movie = movieRepository.findById(dto.getMovieId())
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MovieReviewID id = new MovieReviewID(dto.getMovieId(), userId);

        boolean isNewReview = !reviewRepository.existsById(id);

        MovieReview review = reviewRepository.findById(id)
                .orElse(MovieReview.builder()
                        .movieReviewID(id)
                        .movie(movie)
                        .reviewer(user)
                        .build());

        Integer oldRating = review.getRating();
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        if (movie.getRatingCount() == null) movie.setRatingCount(0);
        if (movie.getRatingSum() == null) movie.setRatingSum(0L);

        if (isNewReview) {
            movie.setRatingCount(movie.getRatingCount() + 1);
            movie.setRatingSum(movie.getRatingSum() + dto.getRating());
        } else {
            movie.setRatingSum(movie.getRatingSum() - oldRating + dto.getRating());
        }

        double avg = (double) movie.getRatingSum() / movie.getRatingCount();
        movie.setAverageRating(Math.round(avg * 10.0) / 10.0);

        movieRepository.save(movie);

        return reviewRepository.save(review);
    }

    @Transactional
    public Page<MovieReview> getMovieReviews(Long movieId, Pageable pageable) {
        return reviewRepository.findByMovie_MovieID(movieId, pageable);
    }

    @Transactional
    public Page<MovieReview> getMyMovieReviews(Long movieId, Pageable pageable) {
        return reviewRepository.findAllByReviewer_Id(movieId, pageable);
    }

    @Transactional
    public Page<MovieReview> getOtherUserMovieReviews(Long userId, Pageable pageable) {
        User  user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if(user.getIsPublic()){
            return reviewRepository.findAllByReviewer_Id(userId, pageable);
        }
        throw new PrivateProfileException("this profile is private");
    }
}
