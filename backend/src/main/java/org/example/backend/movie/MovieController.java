package org.example.backend.movie;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/movie")
public class MovieController {
    @Autowired
    private MovieService movieService;

    @PostMapping("/v1/search")
    public ResponseEntity<Page<MovieDetailsDTO>> searchMoviesPost(
            HttpServletRequest request,
            @Valid @RequestBody MovieRequestDTO requestDTO) {

        Page<MovieDetailsDTO> movies = movieService.getMovies(requestDTO);
        return ResponseEntity.ok(movies);
    }

    @PostMapping("/v1/get-specific-movie/{movieId}")
    public ResponseEntity<MovieDetailsDTO> getSpecificMovie(
            HttpServletRequest request,
            @PathVariable Long movieId){

        MovieDetailsDTO movie = movieService.getMovie(movieId);
        log.debug("Fetched movie: {}", movie.getName());

        return ResponseEntity.ok(movie);
    }


}
