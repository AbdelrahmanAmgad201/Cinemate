package org.example.backend.verification;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.verification.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@EnableScheduling
@Service
@Slf4j
public class VerificationCleanupScheduler {

    @Autowired
    private VerificationService verificationService;

    @Scheduled(fixedRate = 600_000)
    public void cleanupOldVerifications() {
        try {
            verificationService.deleteOldVerifications();
        } catch (Exception e) {
            log.error("Verification cleanup run failed", e);
        }
    }
}
