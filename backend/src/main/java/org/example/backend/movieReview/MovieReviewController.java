package org.example.backend.movieReview;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movie-review")
public class MovieReviewController {

    @Autowired
    private MovieReviewService movieReviewService;

    @PostMapping("/v1/add-review")
    public ResponseEntity<MovieReviewDetailsDTO> addOrUpdateReview(
            HttpServletRequest request,
            @Valid @RequestBody MovieReviewDTO movieReviewDTO) {

        Long userId = (Long) request.getAttribute("userId");

        return ResponseEntity.ok(
                movieReviewService.addOrUpdateReview(userId, movieReviewDTO)
        );
    }
    @GetMapping("/v1/get-movie-reviews/{movieId}")
    public ResponseEntity<Page<MovieReviewDetailsDTO>> getMovieReviews(
            @PathVariable Long movieId,
            Pageable pageable) {

        return ResponseEntity.ok(
                movieReviewService.getMovieReviews(movieId, pageable)
        );
    }

    @GetMapping("/v1/my-movie-review")
    public ResponseEntity<Page<MovieReviewDetailsDTO>> getMyMovieReviews(
            HttpServletRequest request,
            @PageableDefault Pageable pageable
    ){
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(movieReviewService.getMyMovieReviews(userId, pageable));
    }

    @GetMapping("/v1/other-user-movie-review/{userId}")
    public ResponseEntity<Page<MovieReviewDetailsDTO>> getOtherUserMovieReviews(
            @PathVariable Long userId,
            Pageable pageable
    ){
        return ResponseEntity.ok(movieReviewService.getOtherUserMovieReviews(userId, pageable));
    }
}


