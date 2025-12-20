package org.example.backend.user;

import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.example.backend.security.CredentialsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/v1/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("userEmail");

        return ResponseEntity.ok("User profile for ID: " + userId + ", Email: " + email);
    }

    @GetMapping("/v1/profile/{userId}")
    public ResponseEntity<UserProfileResponseDTO> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PostMapping("/v1/set-user-data")
    public ResponseEntity<String> setPersonalData(
            HttpServletRequest request,
            @RequestBody UserDataDTO userDataDTO) {

        Long userId = (Long) request.getAttribute("userId");
        String message = userService.setUserData(userId, userDataDTO);

        return ResponseEntity.ok(message);
    }

    @GetMapping("/v1/user-name-from-object-user-id/{userId}")
    public ResponseEntity<String> getUserNameFromObjectUserId(
            HttpServletRequest request,
            @PathVariable ObjectId userId) {
        return  ResponseEntity.ok(userService.getUserNameFromObjectUserId(userId));
    }

    @PatchMapping("/v1/complete-profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> completeProfile(
        @RequestBody ProfileCompletionDTO request,
        HttpServletRequest httpRequest){

        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(userService.completeProfile(userId, request));
    }


    @GetMapping("/test")
    public ResponseEntity<String> testUser() {
        return ResponseEntity.ok("USER OK");
    }
}