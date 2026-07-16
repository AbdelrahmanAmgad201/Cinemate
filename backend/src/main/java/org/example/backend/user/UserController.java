package org.example.backend.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.backend.common.dto.AboutDTO;
import org.example.backend.security.CredentialsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/v1/profile")
    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("userEmail");

        return ResponseEntity.ok("User profile for ID: " + userId + ", Email: " + email);
    }

    @GetMapping("/v1/profile/{userId}")
    public ResponseEntity<UserProfileResponseDTO> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PutMapping("/v1/profile-data")
    public ResponseEntity<String> setPersonalData(
            HttpServletRequest request,
            @Valid @RequestBody UserDataDTO userDataDTO) {

        Long userId = (Long) request.getAttribute("userId");
        String message = userService.updateUserData(userId, userDataDTO);

        return ResponseEntity.ok(message);
    }

    // Resolves a user id to a display name; ids are plain numeric user ids.
    @GetMapping("/v1/name/{userId}")
    public ResponseEntity<String> getUserName(
            HttpServletRequest request,
            @PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserName(userId));
    }

    @GetMapping("/v1/is-public")
    public ResponseEntity<Boolean> getIsPublic(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(userService.isPublic(userId));
    }

    @PutMapping("/v1/is-public")
    public ResponseEntity<?> isPublic(HttpServletRequest request,
                                      @RequestBody Boolean isPublic) {
        Long userId = (Long) request.getAttribute("userId");
        userService.setIsPublic(userId, isPublic);
        return ResponseEntity.ok("User has been updated successfully");
    }
    @PatchMapping("/v1/complete-profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> completeProfile(
        @Valid @RequestBody ProfileCompletionDTO request,
        HttpServletRequest httpRequest){

        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(userService.completeProfile(userId, request));
    }


    @GetMapping("/test")
    public ResponseEntity<String> testUser() {
        return ResponseEntity.ok("USER OK");
    }

    @PutMapping("/v1/about")
    public ResponseEntity<String> updateAboutUser(HttpServletRequest request,
                                            @Valid @RequestBody AboutDTO about) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateAbout(userId, about);
        return ResponseEntity.ok("about is updated successfully");
    }

    @PutMapping("/v1/birth-date")
    public ResponseEntity<String> updateBirthDate(HttpServletRequest request,
                                                  @Valid @RequestBody BirthDateDTO birthDate){
        Long userId = (Long) request.getAttribute("userId");
        userService.updateBirthDate(userId, birthDate);
        return ResponseEntity.ok("Birth Date is updated successfully");
    }

    @PutMapping("/v1/name")
    public ResponseEntity<String> updateUserName(HttpServletRequest request,
                                                 @Valid @RequestBody UserName userName) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateName(userId,userName);
        return ResponseEntity.ok("User name is updated successfully");
    }
}