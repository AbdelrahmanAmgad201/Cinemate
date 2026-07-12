package org.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Server-side store for the long-lived, opaque refresh tokens (SEC-01), backed by the
 * {@code refresh_tokens} table (was Redis).
 *
 * <ul>
 *   <li><b>Opaque, not a JWT.</b> 256 bits of randomness, invalidated instantly by
 *       deleting the row.</li>
 *   <li><b>Hashed at rest.</b> Only the SHA-256 of the token is stored (SEC-10).</li>
 *   <li><b>Single-use rotation.</b> {@link #rotate} deletes the presented token and issues
 *       a fresh one; the bulk delete's row count is the serialization point, so a token
 *       presented twice fails the second time (the Redis GETDEL guarantee, in SQL).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    @Value("${jwt.refresh-token-expiration-ms:604800000}") // 7 days
    private long refreshTokenExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    /** Identity re-hydrated from a valid refresh token, plus the rotated replacement. */
    public record RefreshResult(String email, String role, String newRefreshToken) {}

    /** Issues a new refresh token bound to the account; returns the raw token for the cookie. */
    @Transactional
    public String issue(String email, String role) {
        String rawToken = randomToken();
        Instant now = Instant.now();
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .role(role)
                .email(email)
                .expiresAt(now.plusMillis(refreshTokenExpirationMs))
                .createdAt(now)
                .build());
        return rawToken;
    }

    /**
     * Validates and consumes a refresh token, issuing a replacement. Empty if the token is
     * unknown, expired, already-rotated, revoked, or forged.
     */
    @Transactional
    public Optional<RefreshResult> rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String hash = hash(rawToken);
        RefreshToken token = refreshTokenRepository.findById(hash).orElse(null);
        if (token == null || token.getExpiresAt().isBefore(Instant.now())) {
            if (token != null) {
                refreshTokenRepository.deleteByHash(hash); // purge the expired row
            }
            return Optional.empty();
        }
        // Single-use: of two concurrent rotations, only one deletes a row.
        if (refreshTokenRepository.deleteByHash(hash) == 0) {
            return Optional.empty();
        }
        String newToken = issue(token.getEmail(), token.getRole());
        return Optional.of(new RefreshResult(token.getEmail(), token.getRole(), newToken));
    }

    /** Invalidates a refresh token (logout). No-op if it's already gone. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            refreshTokenRepository.deleteByHash(hash(rawToken));
        }
    }

    private static String randomToken() {
        byte[] bytes = new byte[32]; // 256 bits
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
