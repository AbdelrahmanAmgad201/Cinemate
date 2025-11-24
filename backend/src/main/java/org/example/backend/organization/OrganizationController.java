package org.example.backend.organization;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.movie.MovieAddDTO;
import org.example.backend.movie.MovieService;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsRepository;
import org.example.backend.requests.RequestsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private MovieService movieService;
    @Autowired
    private RequestsRepository requestsRepository;
    @Autowired
    private RequestsService requestsService;

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
    public Long addMovie(@RequestBody MovieAddDTO movieAddDTO) {
        return organizationService.requestMovie(movieAddDTO);
    }

    @PostMapping("/v1/get_org_rquests")
    public List<Requests> getOrgRequests(@RequestParam Long orgId) {
        return requestsService.getAllOrganizationRequests(orgId);
    }

}