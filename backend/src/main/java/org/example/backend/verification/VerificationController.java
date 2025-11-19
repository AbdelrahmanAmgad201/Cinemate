package org.example.backend.verification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verification")
public class VerificationController  {
    @Autowired
    private VerificationService verificationService;

    @PostMapping("/v1/verify")
    public boolean verify(@RequestBody VerificationDTO verificationDTO) {
        return verificationService.verifyEmail(verificationDTO);
    }
}
