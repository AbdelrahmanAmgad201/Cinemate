package org.example.backend.requests;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@EnableScheduling
@Service
@RequiredArgsConstructor
public class RequestsCleanupScheduler {

    private final RequestsService requestsService;

    // Runs every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupOldDeclineMails() {
        requestsService.deleteOldRequests();
    }
}
