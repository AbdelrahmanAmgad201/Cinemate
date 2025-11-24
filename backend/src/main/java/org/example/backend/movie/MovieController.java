package org.example.backend.movie;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.user.UserDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movie")
public class MovieController {
    @Autowired
    private MovieService movieService;

    @PostMapping("/v1/search")
    public ResponseEntity<Page<Movie>> searchMoviesPost(
            HttpServletRequest request,
            @RequestBody MovieRequestDTO requestDTO) {

        Page<Movie> movies = movieService.getMovies(requestDTO);
        return ResponseEntity.ok(movies);
    }


}
