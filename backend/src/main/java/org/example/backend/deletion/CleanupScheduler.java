package org.example.backend.deletion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final CascadeDeletionService deletionService;

    /**
     * Run once daily at 03:00 AM
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    public void cleanupSoftDeletedData() {
        log.info("[Cleanup] Running daily cleanup job...");

        deletionService.hardDeleteOldEntities("forums",   30);
        deletionService.hardDeleteOldEntities("posts",    30);
        deletionService.hardDeleteOldEntities("comments", 30);
        deletionService.hardDeleteOldEntities("votes",    30);

        log.info("[Cleanup] Completed cleanup job.");
    }
}

