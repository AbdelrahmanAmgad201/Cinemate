package org.example.backend.deletion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.security.OAuthExchangeCodeRepository;
import org.example.backend.security.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final CascadeDeletionService deletionService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthExchangeCodeRepository oauthExchangeCodeRepository;

    private static final int RETENTION_DAYS = 30;

    /** Physically purge content soft-deleted more than RETENTION_DAYS ago. Child-first so
     *  FK ON DELETE CASCADE and the per-table purge don't fight over the same rows. */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void cleanupSoftDeletedData() {
        log.info("[Cleanup] Running daily purge job...");
        deletionService.purgeOldComments(RETENTION_DAYS);
        deletionService.purgeOldPosts(RETENTION_DAYS);
        deletionService.purgeOldForums(RETENTION_DAYS);
        log.info("[Cleanup] Completed purge job.");
    }

    /** Expired auth tokens (formerly Redis TTL). Small, high-churn — swept hourly. */
    @Scheduled(fixedDelayString = "${cleanup.token-sweep-ms:3600000}")
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int tokens = refreshTokenRepository.deleteExpired(now);
        int codes = oauthExchangeCodeRepository.deleteExpired(now);
        if (tokens > 0 || codes > 0) {
            log.info("[Cleanup] Removed {} expired refresh tokens, {} expired OAuth codes", tokens, codes);
        }
    }
}
