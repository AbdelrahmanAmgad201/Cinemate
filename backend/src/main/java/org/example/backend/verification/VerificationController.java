package org.example.backend.verification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/verification")
public class VerificationController  {
    @Autowired
    private VerificationService verificationService;


    @PostMapping("/v1/verify")
    public Map<String, Object> verify(@RequestBody VerificationDTO verificationDTO) {
        boolean success = verificationService.verifyEmail(verificationDTO);

        return Map.of(
                "success", success,
                "message", success ? "Verification successful" : "Invalid or expired code",
                "data", success
        );
    }

}
