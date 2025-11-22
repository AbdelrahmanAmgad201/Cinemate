package org.example.backend.organization;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @GetMapping("/v1/profile")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("userEmail");

        return ResponseEntity.ok("User profile for ID: " + userId + ", Email: " + email);
    }


    @PostMapping("/v1/set-organization-data")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<String> setPersonalData(
            HttpServletRequest request,
            @RequestBody OrganizationDataDTO organizationDataDTO) {

        Long userId = (Long) request.getAttribute("userId");
        String message = organizationService.setOrganizationData(userId, organizationDataDTO);

        return ResponseEntity.ok(message);
    }
}