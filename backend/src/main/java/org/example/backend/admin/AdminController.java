package org.example.backend.admin;

import lombok.RequiredArgsConstructor;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieService;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final MovieService movieService;
    private final AdminService adminService;
    private final RequestsService requestsService;

    @PostMapping("/v1/find_admin_requests")
    public ResponseEntity<List<Movie>> findAllAdminRequests() {
        return ResponseEntity.ok(movieService.findAllAdminRequests());
    }

    @PostMapping("/v1/decline_request")
    public ResponseEntity<Void> declineMovie(@RequestParam Long requestId) {
        adminService.declineRequest(requestId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/v1/accept_request")
    public ResponseEntity<Void> acceptMovie(@RequestBody AcceptDTO acceptDTO) {
        adminService.acceptRequests(acceptDTO);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/v1/get_pending_requests")
    public ResponseEntity<List<Requests>> findAllPendingRequests() {
        return ResponseEntity.ok(requestsService.getAllPendingRequests());
    }

    @PostMapping("/v1/get_requested_movie")
    public ResponseEntity<Movie> getRequestedMovie(@RequestParam Long requestId) {
        return ResponseEntity.ok(adminService.getRequestedMovie(requestId));
    }
}
