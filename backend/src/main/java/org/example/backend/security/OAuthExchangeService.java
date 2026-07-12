package org.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Short-lived, single-use exchange codes for the OAuth redirect handoff (SEC-04): keeps
 * the JWT out of the URL. Backed by the {@code oauth_exchange_codes} table (was Redis) so
 * the redirect and the frontend's exchange callback can land on different backend replicas.
 * Redeem deletes the row and checks the row count, so a code can be redeemed at most once.
 */
@Service
@RequiredArgsConstructor
public class OAuthExchangeService {

    private static final long CODE_TTL_SECONDS = 30;

    private final OAuthExchangeCodeRepository codeRepository;

    @Transactional
    public String issueCode(String token) {
        UUID code = UUID.randomUUID();
        codeRepository.save(OAuthExchangeCode.builder()
                .code(code)
                .token(token)
                .expiresAt(Instant.now().plus(CODE_TTL_SECONDS, ChronoUnit.SECONDS))
                .build());
        return code.toString();
    }

    @Transactional
    public String redeemCode(String code) {
        UUID id;
        try {
            id = UUID.fromString(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
        OAuthExchangeCode entry = codeRepository.findById(id).orElse(null);
        if (entry == null) {
            return null;
        }
        // Single-use: only the caller whose delete affects a row may redeem it.
        if (codeRepository.deleteByCode(id) == 0) {
            return null;
        }
        if (entry.getExpiresAt().isBefore(Instant.now())) {
            return null;
        }
        return entry.getToken();
    }
}
