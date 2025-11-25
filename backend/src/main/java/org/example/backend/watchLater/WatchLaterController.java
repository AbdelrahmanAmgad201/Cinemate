package org.example.backend.watchLater;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.likedMovie.LikedMovie;
import org.example.backend.likedMovie.LikedMovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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



}
