package org.example.backend.likedMovie;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/liked-movie")
public class LikedMovieController {
    @Autowired
    private LikedMovieService likedMovieService;

    @PostMapping("/v1/like-movie/{movieId}")
    public ResponseEntity<LikedMovie> likeMovie(
            HttpServletRequest request,
            @PathVariable Long movieId) {

        Long userId = (Long) request.getAttribute("userId");
//        System.out.println("userId = " + userId);
        return ResponseEntity.ok(
                likedMovieService.likeMovie(userId, movieId)
        );
    }

    @DeleteMapping("/v1/like-movie/{movieId}")
    public ResponseEntity<?> deleteMovie(
            HttpServletRequest request,
            @PathVariable Long movieId
    ) {
        Long userId = (Long) request.getAttribute("userId");
        likedMovieService.unlikeMovie(userId, movieId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/v1/my-liked-movies")
    public ResponseEntity<Page<LikedMovieView>> getMyLikedMovies(HttpServletRequest request,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(likedMovieService.getMyLikedMovies(userId,pageable));
    }

    @GetMapping("/v1/other-user-liked-movies/{userId}")
    public ResponseEntity<Page<LikedMovieView>> getOtherUserLikedMovies(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ){
        return ResponseEntity.ok(likedMovieService.getOtherUserLikedMovies(userId,pageable));
    }

    @GetMapping("/v1/is-liked/{movieId}")
    public ResponseEntity<Boolean> isLiked(
            HttpServletRequest request,
            @PathVariable Long movieId
    ){
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(likedMovieService.isLiked(userId,movieId));
    }

}

