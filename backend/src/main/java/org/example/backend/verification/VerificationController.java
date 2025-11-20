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
        boolean result = verificationService.verifyEmail(verificationDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("email", verificationDTO.getEmail());
        response.put("code", verificationDTO.getCode());
        response.put("success", result);
        response.put("message", result ? "Verification successful" : "Invalid or expired code");
        response.put("data", result);

        return response;
    }

}
