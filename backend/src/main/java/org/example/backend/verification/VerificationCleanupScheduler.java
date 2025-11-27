package org.example.backend.verification;

import org.example.backend.verification.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@EnableScheduling
@Service
public class VerificationCleanupScheduler {

    @Autowired
    private VerificationService verificationService;

    @Scheduled(fixedRate = 600_000)
    public void cleanupOldVerifications() {
        verificationService.deleteOldVerifications();
    }
}
