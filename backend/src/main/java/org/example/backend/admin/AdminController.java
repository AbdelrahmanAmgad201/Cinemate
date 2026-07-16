package org.example.backend.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.dto.UpdateNameRequest;
import org.example.backend.movie.MovieDetailsDTO;
import org.example.backend.movie.MovieService;
import org.example.backend.movie.OneMovieOverView;
import org.example.backend.requests.RequestsResponse;
import org.example.backend.requests.RequestsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {


    private final AdminService adminService;
    private final RequestsService requestsService;
    private final MovieService movieService;

    @GetMapping("/v1/my-requests")
    public ResponseEntity<List<RequestsResponse>> findAllAdminRequests(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(requestsService.getAllAdminRequests(userId));
    }

    @PostMapping("/v1/requests/{requestId}/decline")
    public ResponseEntity<String> declineRequest(HttpServletRequest request, @PathVariable Long requestId) {
        Long userId = (Long) request.getAttribute("userId");
        adminService.declineRequest(userId, requestId);
        return ResponseEntity.ok("Request " + requestId + " declined successfully by admin " + userId);
    }

    @PostMapping("/v1/requests/{requestId}/accept")
    public ResponseEntity<String> acceptRequest(HttpServletRequest request, @PathVariable Long requestId) {
        Long userId = (Long) request.getAttribute("userId");
        adminService.acceptRequests(userId, requestId);
        return ResponseEntity.ok("Request " + requestId + " accepted successfully by admin " + userId);
    }

    @GetMapping("/v1/pending-requests")
    public ResponseEntity<List<RequestsResponse>> findAllPendingRequests(HttpServletRequest request) {
        return ResponseEntity.ok(requestsService.getAllPendingRequests());
    }

    @GetMapping("/v1/requests/{requestId}/movie")
    public ResponseEntity<MovieDetailsDTO> getRequestedMovie(HttpServletRequest request, @PathVariable Long requestId) {
        return ResponseEntity.ok(adminService.getRequestedMovie(requestId));
    }

    @GetMapping("/v1/movies/{movieId}/overview")
    public ResponseEntity<OneMovieOverView> getSpecificMovieOverview(HttpServletRequest request, @PathVariable Long movieId) {
            OneMovieOverView overview = movieService.getMovieStatsByMovieId(movieId);
            return ResponseEntity.ok(overview);
    }

    @GetMapping("/v1/system-overview")
    public ResponseEntity<SystemOverview> getSystemOverview(HttpServletRequest request) {
        return ResponseEntity.ok().body(adminService.getSystemOverview());
    }
    @PostMapping("/v1/admins")
    public ResponseEntity<?> addAdmin(HttpServletRequest request,@Valid @RequestBody AddAdminDTO addAdminDTO) {
        adminService.addAdmin(addAdminDTO);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/v1/profile")
    public ResponseEntity<AdminProfileDTO> getAdminProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        return ResponseEntity.ok(adminService.getAdminProfile(userId));
    }

    @PutMapping("/v1/name")
    public ResponseEntity<String> updateAdminName(HttpServletRequest request, @Valid @RequestBody UpdateNameRequest nameRequest) {
        Long adminId = (Long) request.getAttribute("userId");

        adminService.updateAdminName(adminId, nameRequest.getName());
        return ResponseEntity.ok("Name updated successfully");
    }
}
