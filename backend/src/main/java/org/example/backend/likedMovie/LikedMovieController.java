package org.example.backend.likedMovie;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}

