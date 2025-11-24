package org.example.backend.movieReview;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRequestDTO;
import org.example.backend.movie.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movie-review")
public class MovieReviewController {

    @Autowired
    private MovieReviewService movieReviewService;

    @PostMapping("/v1/add-review")
    public ResponseEntity<MovieReview> addOrUpdateReview(
            HttpServletRequest request,
            @RequestBody MovieReviewDTO movieReviewDTO) {

        Long userId = (Long) request.getAttribute("userId");

        return ResponseEntity.ok(
                movieReviewService.addOrUpdateReview(userId, movieReviewDTO)
        );
    }
}


