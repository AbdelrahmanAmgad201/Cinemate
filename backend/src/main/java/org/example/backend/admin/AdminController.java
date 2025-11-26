package org.example.backend.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.backend.movie.Movie;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {


    private final AdminService adminService;
    private final RequestsService requestsService;

    @PostMapping("/v1/find-admin-requests")
    public ResponseEntity<List<Requests>> findAllAdminRequests(HttpServletRequest request,@RequestParam Long adminId) {
        return ResponseEntity.ok(requestsService.getAllAdminRequests(adminId));
    }

    @PostMapping("/v1/decline-request")
    public ResponseEntity<String> declineRequest(HttpServletRequest request, @RequestParam Long requestId) {
        Long userId = (Long) request.getAttribute("userId");

        try {
            adminService.declineRequest(userId, requestId);
            return ResponseEntity.ok("Request " + requestId + " declined successfully by admin " + userId);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to decline request " + requestId + ": " + e.getMessage());
        }
    }

    @PostMapping("/v1/accept-request")
    public ResponseEntity<String> acceptRequest(HttpServletRequest request, @RequestParam Long requestId) {
        Long userId = (Long) request.getAttribute("userId");

        try {
            adminService.acceptRequests(userId, requestId);
            return ResponseEntity.ok("Request " + requestId + " accepted successfully by admin " + userId);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to accept request " + requestId + ": " + e.getMessage());
        }
    }

    @PostMapping("/v1/get-pending-requests")
    public ResponseEntity<List<Requests>> findAllPendingRequests(HttpServletRequest request) {
        return ResponseEntity.ok(requestsService.getAllPendingRequests());
    }

    @PostMapping("/v1/get-requested-movie")
    public ResponseEntity<Movie> getRequestedMovie(HttpServletRequest request,@RequestParam Long requestId) {
        return ResponseEntity.ok(adminService.getRequestedMovie(requestId));
    }
}
