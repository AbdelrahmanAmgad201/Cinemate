package org.example.backend.watchHistory;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.movieReview.MovieReview;
import org.example.backend.movieReview.MovieReviewDTO;
import org.example.backend.movieReview.MovieReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/watch-history")
public class WatchHistoryController {

    @Autowired
    private WatchHistoryService watchHistoryService;

    @PostMapping("/v1/add-watch-history/{movieId}")
    public ResponseEntity<WatchHistory> likeMovie(
            HttpServletRequest request,
            @PathVariable Long movieId) {

        Long userId = (Long) request.getAttribute("userId");
//        System.out.println("userId = " + userId);
        return ResponseEntity.ok(
                watchHistoryService.addToWatchHistory(userId, movieId)
        );
    }

}