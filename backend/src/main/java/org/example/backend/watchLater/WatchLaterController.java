package org.example.backend.watchLater;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.likedMovie.LikedMovie;
import org.example.backend.likedMovie.LikedMovieService;
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

    @PostMapping("/v1/watch-later/{movieId}")
    public ResponseEntity<WatchLater> addToWatchLater(
            HttpServletRequest request,
            @PathVariable Long movieId) {

        Long userId = (Long) request.getAttribute("userId");
//        System.out.println("userId = " + userId);
        return ResponseEntity.ok(
                watchLaterService.addMovie(userId, movieId)
        );
    }

    @GetMapping("/v1/watch-later")
    public ResponseEntity<Page<WatchLaterView>> getWatchLater(HttpServletRequest request,
                                                              @PageableDefault(size = 20) Pageable pageable){
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(watchLaterService.getWatchLaters(userId, pageable));
    }

    @DeleteMapping("/v1/watch-later/{movieId}")
    public ResponseEntity<?>  deleteWatchLater(HttpServletRequest request, @PathVariable Long movieId) {
        Long userId = (Long) request.getAttribute("userId");
        watchLaterService.deleteWatchLater(userId,movieId);
        return ResponseEntity.ok().build();
    }

}
