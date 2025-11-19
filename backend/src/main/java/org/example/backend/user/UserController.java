package org.example.backend.user;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.security.CredentialsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private  UserService userService;

    @PostMapping("/v1/sign-up")
    public User signUp(@RequestBody CredentialsRequest credentialsRequest) {
        return userService.signUp(credentialsRequest);
    }

    @GetMapping("/v1/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("userEmail");

        return ResponseEntity.ok("User profile for ID: " + userId + ", Email: " + email);
    }
}