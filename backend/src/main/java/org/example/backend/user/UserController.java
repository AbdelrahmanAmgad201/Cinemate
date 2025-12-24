package org.example.backend.user;

import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
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

    @GetMapping("/test")
    public ResponseEntity<String> testUser() {
        return ResponseEntity.ok("USER OK");
    }

    @PutMapping("/v1/user-about")
    public ResponseEntity<String> updateAboutUser(HttpServletRequest request,
                                            @RequestBody AboutDTO about) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateAbout(userId, about);
        return ResponseEntity.ok("about is updated successfully");
    }

    @PutMapping("/v1/user-birth-date")
    public ResponseEntity<String> updateBirthDate(HttpServletRequest request,
                                                  @RequestBody BirthDateDTO birthDate){
        Long userId = (Long) request.getAttribute("userId");
        userService.updateBirthDate(userId, birthDate);
        return ResponseEntity.ok("Birth Date is updated successfully");
    }

    @PutMapping("/v1/user-name")
    public ResponseEntity<String> updateUserName(HttpServletRequest request,
                                                 @RequestBody UserName userName) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateName(userId,userName);
        return ResponseEntity.ok("User name is updated successfully");
    }
}