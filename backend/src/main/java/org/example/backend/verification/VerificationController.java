package org.example.backend.verification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<VerificationResponseDTO> verify(@RequestBody VerificationDTO verificationDTO) {
        VerificationResponseDTO response = verificationService.verifyEmail(verificationDTO);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

}
