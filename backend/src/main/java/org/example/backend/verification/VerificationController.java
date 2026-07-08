package org.example.backend.verification;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.backend.security.RefreshTokenCookie;
import org.example.backend.security.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/verification")
public class VerificationController  {
    @Autowired
    private VerificationService verificationService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenCookie refreshTokenCookie;


    @PostMapping("/v1/verify")
    public ResponseEntity<VerificationResponseDTO> verify(@Valid @RequestBody VerificationDTO verificationDTO) {
        VerificationResponseDTO response = verificationService.verifyEmail(verificationDTO);

        if (response.isSuccess()) {
            // Verifying the email logs the account in, so issue the refresh cookie
            // here (the access token is already in the response body) — same handoff
            // as /login, done at the controller layer where the cookie can be set.
            String refreshToken = refreshTokenService.issue(response.getEmail(), response.getRole());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.build(refreshToken).toString())
                    .body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/v1/verify-forget-password/{code}")
    public ResponseEntity<Boolean> verifyForgetPassword(HttpServletRequest request,
                                                        @PathVariable int code) {
        String email = (String) request.getAttribute("userEmail");
        return ResponseEntity.ok(verificationService.forgetPasswordVerification(email, code));
    }

    @PutMapping("/v1/update-password-with-code")
    public ResponseEntity<String> updatePasswordWithVerificationCode(HttpServletRequest request,
                @Valid @RequestBody UpdatePasswordWithVerificationDTO updatePasswordWithVerificationDTO) {
        String email = (String) request.getAttribute("userEmail");
        String role = (String) request.getAttribute("userRole");
        verificationService.updatePasswordByVerificationCode(email,updatePasswordWithVerificationDTO,role);
        return ResponseEntity.ok("password updated successfully");
    }

    @PostMapping("/v1/forget-password")
    public ResponseEntity<String> forgetPassword(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        String  role = (String) request.getAttribute("userRole");
        verificationService.forgetPassword(email,role);
        return ResponseEntity.ok("verification code updated successfully");
    }

}
