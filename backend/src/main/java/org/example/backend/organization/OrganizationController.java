package org.example.backend.organization;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.movie.*;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private RequestsService requestsService;
    @Autowired
    private MovieService movieService;

    @GetMapping("/v1/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("userEmail");

        return ResponseEntity.ok("User profile for ID: " + userId + ", Email: " + email);
    }


    @PostMapping("/v1/set-organization-data")
    public ResponseEntity<String> setPersonalData(
            HttpServletRequest request,
            @RequestBody OrganizationDataDTO organizationDataDTO) {

        Long userId = (Long) request.getAttribute("userId");
        String message = organizationService.setOrganizationData(userId, organizationDataDTO);

        return ResponseEntity.ok(message);
    }

    @PostMapping("/v1/add-movie")
    public ResponseEntity<?> addMovie(HttpServletRequest request, @RequestBody MovieAddDTO movieAddDTO) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            Long movieId = organizationService.requestMovie(userId, movieAddDTO);

            return ResponseEntity.ok().body(
                    Map.of(
                            "success", true,
                            "message", "Movie request submitted successfully",
                            "movieId", movieId
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of(
                            "success", false,
                            "message", "Failed to request movie: " + e.getMessage()
                    )
            );
        }
    }


    @PostMapping("/v1/get-all-organization-requests")
    public List<Requests> getOrgRequests(HttpServletRequest request) {
        Long orgId = (Long) request.getAttribute("userId");
        return requestsService.getAllOrganizationRequests(orgId);
    }

    @PostMapping("/v1/movies-overview")
    public ResponseEntity<MoviesOverview> getMoviesOverview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok().body(movieService.getMoviesOverview(userId));
    }

    @PostMapping("/v1/get-organization-movies")
    public ResponseEntity<List<Movie>> getOrganizationMovies(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok().body(movieService.getOrganizationMovies(userId));
    }

    @PostMapping("/v1/get-specific-movie-overview")
    public ResponseEntity<OneMovieOverView> getSpecificMovieOverview(HttpServletRequest request,
                                                                     @RequestParam Long movieId) {
        Long userId = (Long) request.getAttribute("userId");

        if (movieService.OrganizationOwnMovie(userId, movieId)) {
            OneMovieOverView overview = movieService.getMovieStatsByMovieId(movieId);
            return ResponseEntity.ok(overview);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/v1/get-requests-over-view")
    public ResponseEntity<RequestsOverView> getRequestsOverview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok().body(requestsService.getRequestsOverView(userId));
    }

    @GetMapping("/v1/personal-data")
    public ResponseEntity<PersonalData> getPersonalData(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(organizationService.getPersonalData(userId));
    }
    @GetMapping("/v1/my-movies")
    public ResponseEntity<Page<MovieView>> getMyMovies(
            HttpServletRequest request,
            @PageableDefault(size = 20) Pageable pageable) {
        Long orgId = (Long) request.getAttribute("userId");
        return  ResponseEntity.ok(movieService.getOrganizationMovies(orgId, pageable));
    }

}