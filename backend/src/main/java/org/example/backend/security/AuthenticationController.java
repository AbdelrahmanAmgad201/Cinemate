package org.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.backend.user.User;
import org.example.backend.user.UserAlreadyExistsException;
import org.example.backend.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.backend.verification.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JWTProvider jwtTokenProvider;
    private final UserService userService;

    @PostMapping("/v1/login")
    public ResponseEntity<?> login(@RequestBody CredentialsRequest request) {
        return authenticationService.authenticate(
                request.getEmail(),
                request.getPassword(),
                request.getRole()
        ).map(account -> {
            String token = jwtTokenProvider.generateToken(account);

            Map<String, Object> response = Map.of(
                    "token", token,
                    "id", account.getId(),
                    "email", account.getEmail(),
                    "role", account.getRole()
            );

            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials")));
    }



    @PostMapping("/v1/sign-up")
    public ResponseEntity<?> signUp(@RequestBody CredentialsRequest credentialsRequest) {
        try {
            Verfication verification = userService.signUp(credentialsRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(verification);
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/v1/password")
    public ResponseEntity<String > updatePassword(
            HttpServletRequest request,
            @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        String email = (String) request.getAttribute("userEmail");
        String role = (String) request.getAttribute("userRole");
        authenticationService.updatePassword(email, updatePasswordDTO, role);
        return ResponseEntity.ok("password updated successfully");
    }


}