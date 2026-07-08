package org.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.user.UserAlreadyExistsException;
import org.example.backend.user.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.backend.verification.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JWTProvider jwtTokenProvider;
    private final UserService userService;
    private final OAuthExchangeService oAuthExchangeService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookie refreshTokenCookie;

    @PostMapping("/v1/login")
    public ResponseEntity<?> login(@Valid @RequestBody CredentialsRequest request) {
        return authenticationService.authenticate(
                request.getEmail(),
                request.getPassword(),
                request.getRole()
        ).<ResponseEntity<?>>map(this::issueTokens)
         .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials")));
    }

    /**
     * Trades a valid refresh-token cookie for a fresh access token and a rotated
     * refresh cookie. The account is reloaded from the store here, so role or
     * profile changes are picked up on the next refresh rather than being frozen
     * into a long-lived token.
     */
    @PostMapping("/v1/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = RefreshTokenCookie.COOKIE_NAME, required = false) String refreshToken) {

        return refreshTokenService.rotate(refreshToken)
                .flatMap(result -> authenticationService
                        .findByEmailAndRole(result.email(), result.role())
                        .<ResponseEntity<?>>map(account -> ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.build(result.newRefreshToken()).toString())
                                .body(accountBody(account, jwtTokenProvider.generateAccessToken(account)))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
                        .body(Map.of("error", "Invalid or expired session")));
    }

    @PostMapping("/v1/logout")
    public ResponseEntity<String> logout(
            @CookieValue(name = RefreshTokenCookie.COOKIE_NAME, required = false) String refreshToken) {
        // Deleting the refresh token stops any further access tokens from being
        // minted. The current access token stays valid until it expires (≤15 min) —
        // the standard, accepted trade-off of dropping the per-request blacklist.
        refreshTokenService.revoke(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
                .body("logged out successfully");
    }

    @PostMapping("/v1/oauth-token")
    public ResponseEntity<?> exchangeOAuthToken(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String accessToken = code != null ? oAuthExchangeService.redeemCode(code) : null;

        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired code"));
        }

        String email = jwtTokenProvider.getEmailFromToken(accessToken);
        String role = jwtTokenProvider.getRoleFromToken(accessToken);
        String refreshToken = refreshTokenService.issue(email, role);

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("id", jwtTokenProvider.getIdFromToken(accessToken));
        response.put("email", email);
        response.put("name", jwtTokenProvider.getNameFromToken(accessToken));
        response.put("role", role);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.build(refreshToken).toString())
                .body(response);
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
    public ResponseEntity<String> updatePassword(
            HttpServletRequest request,
            @Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        String email = (String) request.getAttribute("userEmail");
        String role = (String) request.getAttribute("userRole");
        authenticationService.updatePassword(email, updatePasswordDTO, role);
        return ResponseEntity.ok("password updated successfully");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Mints an access token + a refresh cookie for a freshly authenticated account. */
    private ResponseEntity<?> issueTokens(Authenticatable account) {
        String accessToken = jwtTokenProvider.generateAccessToken(account);
        String refreshToken = refreshTokenService.issue(account.getEmail(), account.getRole());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.build(refreshToken).toString())
                .body(accountBody(account, accessToken));
    }

    private Map<String, Object> accountBody(Authenticatable account, String accessToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("accessToken", accessToken);
        body.put("id", account.getId());
        body.put("email", account.getEmail());
        body.put("name", account.getName());
        body.put("role", account.getRole());
        return body;
    }
}
