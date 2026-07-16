package org.example.backend.organization;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.backend.common.dto.AboutDTO;
import org.example.backend.common.dto.UpdateNameRequest;
import org.example.backend.movie.*;
import org.example.backend.requests.RequestsResponse;
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
    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("userEmail");

        return ResponseEntity.ok("User profile for ID: " + userId + ", Email: " + email);
    }


    @PutMapping("/v1/profile")
    public ResponseEntity<String> setPersonalData(
            HttpServletRequest request,
            @Valid @RequestBody OrganizationDataDTO organizationDataDTO) {

        Long userId = (Long) request.getAttribute("userId");
        String message = organizationService.updateOrganizationData(userId, organizationDataDTO);

        return ResponseEntity.ok(message);
    }

    @PostMapping("/v1/movies")
    public ResponseEntity<?> addMovie(HttpServletRequest request, @Valid @RequestBody MovieAddDTO movieAddDTO) {
        Long userId = (Long) request.getAttribute("userId");
        Long movieId = organizationService.requestMovie(userId, movieAddDTO);

        return ResponseEntity.ok().body(
                Map.of(
                        "success", true,
                        "message", "Movie request submitted successfully",
                        "movieId", movieId
                )
        );
    }


    @GetMapping("/v1/requests")
    public ResponseEntity<List<RequestsResponse>> getOrgRequests(HttpServletRequest request) {
        Long orgId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(requestsService.getAllOrganizationRequests(orgId));
    }

    @GetMapping("/v1/movies-overview")
    public ResponseEntity<MoviesOverview> getMoviesOverview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok().body(movieService.getMoviesOverview(userId));
    }

    @GetMapping("/v1/movies/{movieId}/overview")
    public ResponseEntity<OneMovieOverView> getSpecificMovieOverview(HttpServletRequest request,
                                                                     @PathVariable Long movieId) {
        Long userId = (Long) request.getAttribute("userId");

        if (movieService.OrganizationOwnMovie(userId, movieId)) {
            OneMovieOverView overview = movieService.getMovieStatsByMovieId(movieId);
            return ResponseEntity.ok(overview);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping("/v1/requests-overview")
    public ResponseEntity<RequestsOverView> getRequestsOverview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok().body(requestsService.getRequestsOverView(userId));
    }

    @GetMapping("/v1/my-movies")
    public ResponseEntity<Page<MovieView>> getMyMovies(
            HttpServletRequest request,
            @PageableDefault(size = 20) Pageable pageable) {
        Long orgId = (Long) request.getAttribute("userId");
        return  ResponseEntity.ok(movieService.getOrganizationMovies(orgId, pageable));
    }

    @GetMapping("/v1/personal-data")
    public ResponseEntity<PersonalData> getPersonalData(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(organizationService.getPersonalData(userId));
    }
    @PutMapping("/v1/about")
    public ResponseEntity<String> about(
            HttpServletRequest request,
            @Valid @RequestBody AboutDTO aboutDTO) {
        Long userId = (Long) request.getAttribute("userId");
        organizationService.updateAbout(userId, aboutDTO);
        return  ResponseEntity.ok("about updated successfully");
    }

    @PutMapping("/v1/name")
    public ResponseEntity<String> updateName(
            HttpServletRequest request,
            @Valid @RequestBody UpdateNameRequest nameRequest){
        Long userId = (Long) request.getAttribute("userId");
        organizationService.updateName(userId, nameRequest.getName());
        return  ResponseEntity.ok("name updated successfully");
    }
    
}