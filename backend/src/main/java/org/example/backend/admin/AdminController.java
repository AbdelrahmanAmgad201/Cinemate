package org.example.backend.admin;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieService;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsService;
import org.example.backend.user.UserDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final MovieService movieService;
    private final AdminService adminService;
    private final RequestsService requestsService;

    @PostMapping("/v1/find_admin_requests")
    public List<Movie> findAllAdminRequests(){
        return movieService.findAllAdminRequests();
    }
    @PostMapping("/v1/decline_request")
    public void declineMovie(@RequestParam Long requestId){
        adminService.declineRequest(requestId);
    }
    @PostMapping("/v1/accept_request")
    public void acceptMovie(@RequestBody AcceptDTO acceptDTO){
        adminService.acceptRequests(acceptDTO);
    }
    @PostMapping("/v1/get_pending_requests")
    public List<Requests> findAllPendingRequests(){
        return requestsService.getAllPendingRequests();
    }
    @PostMapping("/v1/get_requested_movie")
    public Movie getRequestedMovie(@RequestParam Long requestId){
        return adminService.getRequestedMovie(requestId);
    }
}
