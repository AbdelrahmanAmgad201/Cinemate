package org.example.backend.watchHistory;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.movieReview.MovieReview;
import org.example.backend.movieReview.MovieReviewDTO;
import org.example.backend.movieReview.MovieReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping("/v1/watch-history")
    public ResponseEntity<Page<WatchHistoryViewDTO>> getWatchHistory(
            HttpServletRequest request,
            @PageableDefault(size = 20, sort = "watchedAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ){
        Long watcherId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(watchHistoryService.getWatcherWatchHistory(watcherId,pageable));
    }

    @DeleteMapping("/v1/watch-history/{watchHistoryId}")
    public ResponseEntity<?> deleteWatchHistory(HttpServletRequest request,
                                                @PathVariable  Long watchHistoryId) {
        watchHistoryService.removeFromWatchHistory(watchHistoryId);
        return ResponseEntity.ok().build();
    }

}