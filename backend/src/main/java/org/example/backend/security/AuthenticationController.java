package org.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.user.User;
import org.example.backend.user.UserAlreadyExistsException;
import org.example.backend.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.backend.verification.*;

import java.sql.SQLOutput;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JWTProvider jwtTokenProvider;
    private final UserService userService;
    private final OAuthExchangeService oAuthExchangeService;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/v1/login")
    public ResponseEntity<?> login(@Valid @RequestBody CredentialsRequest request) {
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
                    "name", account.getName(),  // Added name to response
                    "role", account.getRole()
            );

            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials")));
    }



    @PostMapping("/v1/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            tokenBlacklistService.revoke(bearerToken.substring(7));
        }
        return ResponseEntity.ok("logged out successfully");
    }

    @PostMapping("/v1/oauth-token")
    public ResponseEntity<?> exchangeOAuthToken(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String token = code != null ? oAuthExchangeService.redeemCode(code) : null;

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired code"));
        }

        Map<String, Object> response = Map.of(
                "token", token,
                "id", jwtTokenProvider.getIdFromToken(token),
                "email", jwtTokenProvider.getEmailFromToken(token),
                "name", jwtTokenProvider.getNameFromToken(token),
                "role", jwtTokenProvider.getRoleFromToken(token)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/v1/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody CredentialsRequest credentialsRequest) {
        try {
            Verification verification = userService.signUp(credentialsRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(verification);
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/v1/password")
    public ResponseEntity<String > updatePassword(
            HttpServletRequest request,
            @Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        String email = (String) request.getAttribute("userEmail");
        String role = (String) request.getAttribute("userRole");
        authenticationService.updatePassword(email, updatePasswordDTO, role);
        return ResponseEntity.ok("password updated successfully");
    }
}