package org.example.backend.verification;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/verification")
public class VerificationController  {
    @Autowired
    private VerificationService verificationService;


    @PostMapping("/v1/verify")
    public ResponseEntity<VerificationResponseDTO> verify(@RequestBody VerificationDTO verificationDTO) {
        VerificationResponseDTO response = verificationService.verifyEmail(verificationDTO);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
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

    @PutMapping("/v1/update-password-with-verification-code")
    public ResponseEntity<String> updatePasswordWithVerificationCode(HttpServletRequest request,
                @RequestBody UpdatePasswordWithVerificationDTO updatePasswordWithVerificationDTO) {
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
