package org.example.backend.movie;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/v1/get-specific-movie")
    public ResponseEntity<Movie> getSpecificPicMovie(
            HttpServletRequest request,
            @RequestParam Long movieId
    ){
        Movie movie = movieService.getMovie(movieId);
        return ResponseEntity.ok(movie);
    }


}
