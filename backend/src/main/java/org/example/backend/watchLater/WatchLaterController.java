package org.example.backend.watchLater;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/watch-later")
public class WatchLaterController {

    @Autowired
    private WatchLaterService watchLaterService;

    @PutMapping("/v1/{movieId}")
    public ResponseEntity<WatchLaterResponse> addToWatchLater(
            HttpServletRequest request,
            @PathVariable Long movieId) {

        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(
                watchLaterService.addMovie(userId, movieId)
        );
    }

    @GetMapping("/v1")
    public ResponseEntity<Page<WatchLaterView>> getWatchLater(HttpServletRequest request,
                                                              @PageableDefault(size = 20) Pageable pageable){
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(watchLaterService.getWatchLaters(userId, pageable));
    }

    @GetMapping("/v1/{movieId}")
    public ResponseEntity<Boolean> isWatchLater(
            HttpServletRequest request,
            @PathVariable Long movieId) {

        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(watchLaterService.isWatchLater(userId, movieId));
    }

    @DeleteMapping("/v1/{movieId}")
    public ResponseEntity<?>  deleteWatchLater(HttpServletRequest request, @PathVariable Long movieId) {
        Long userId = (Long) request.getAttribute("userId");
        watchLaterService.deleteWatchLater(userId,movieId);
        return ResponseEntity.ok().build();
    }

}
