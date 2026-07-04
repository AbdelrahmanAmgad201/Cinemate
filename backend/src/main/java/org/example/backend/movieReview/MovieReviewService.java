package org.example.backend.movieReview;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.errorHandler.ResourceNotFoundException;
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
    public MovieReviewDetailsDTO addOrUpdateReview(Long userId, MovieReviewDTO dto) {

        Movie movie = movieRepository.findById(dto.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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

        // Mapped inside the transaction (CQ-NEW-03) — MovieReviewDetailsDTO.from()
        // touches the lazy movie/reviewer associations.
        return MovieReviewDetailsDTO.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public Page<MovieReviewDetailsDTO> getMovieReviews(Long movieId, Pageable pageable) {
        return reviewRepository.findByMovie_MovieID(movieId, pageable).map(MovieReviewDetailsDTO::from);
    }

    @Transactional(readOnly = true)
    public Page<MovieReviewDetailsDTO> getMyMovieReviews(Long userId, Pageable pageable) {
        return reviewRepository.findAllByReviewer_Id(userId, pageable).map(MovieReviewDetailsDTO::from);
    }


    @Transactional(readOnly = true)
    public Page<MovieReviewDetailsDTO> getOtherUserMovieReviews(Long userId, Pageable pageable) {
        User  user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if(user.getIsPublic()){
            return reviewRepository.findAllByReviewer_Id(userId, pageable).map(MovieReviewDetailsDTO::from);
        }
        throw new PrivateProfileException("this profile is private");
    }
}
