package org.example.backend.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Timer that drives {@link ModerationReconciliationService} (MOD-01). Kept separate from the
 * service so the sweep's own transactions go through the Spring proxy (a scheduled method
 * calling transactional methods on another bean, not on itself — see the self-invocation
 * pitfall this avoids).
 */
@Component
@RequiredArgsConstructor
public class ModerationReconciliationSweep {

    private final ModerationReconciliationService reconciliationService;

    @Value("${moderation.sweep.stuck-after-minutes:10}")
    private long stuckAfterMinutes;

    @Value("${moderation.sweep.batch-size:100}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${moderation.sweep.interval-ms:60000}",
            initialDelayString = "${moderation.sweep.initial-delay-ms:60000}")
    public void sweep() {
        Instant cutoff = Instant.now().minusSeconds(stuckAfterMinutes * 60);
        reconciliationService.sweepPendingPosts(cutoff, batchSize);
        reconciliationService.sweepPendingComments(cutoff, batchSize);
        reconciliationService.sweepPendingForums(cutoff, batchSize);
    }
}
