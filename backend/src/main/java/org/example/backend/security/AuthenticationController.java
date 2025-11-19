package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.security.CredentialsRequest;
import org.example.backend.security.JWTProvider;
import org.example.backend.security.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JWTProvider jwtTokenProvider;

    @PostMapping("/login")
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
}