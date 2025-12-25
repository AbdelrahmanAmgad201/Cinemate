package org.example.backend.movieReview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieReviewRepository extends JpaRepository<MovieReview, MovieReviewID> {

    Page<MovieReview> findByMovieReviewID_MovieId(Long movieId, Pageable pageable);

    boolean existsByMovieReviewID_MovieIdAndMovieReviewID_ReviewerId(Long movieId, Long reviewerId);
    Page<MovieReview> findByMovie_MovieID(Long movieId, Pageable pageable);
    Page<MovieReview> findAllByReviewer_Id(Long reviewerId, Pageable pageable);
}
