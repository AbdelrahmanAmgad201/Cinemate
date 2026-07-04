package org.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * JWT revocation list (SEC-01). JWTs are otherwise stateless and cannot be
 * invalidated before their natural expiry (e.g. on logout). This is the
 * explicit, narrow exception to that statelessness.
 *
 * Backed by Redis (the same "redis-cache" instance as the response cache — see
 * ARC-06) rather than the in-process Caffeine cache this used to be: a revoked
 * token must be rejected by every backend replica, not just the one that
 * processed the logout, or revocation silently stops working the moment the
 * backend is horizontally scaled.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    // Upper bound on JWT lifetime (JWTProvider issues 24h tokens). A revoked entry
    // can safely be forgotten once the token would have expired naturally anyway.
    private static final Duration MAX_TOKEN_LIFETIME = Duration.ofHours(24);
    private static final String KEY_PREFIX = "revoked-token:";

    private final StringRedisTemplate redisTemplate;

    public void revoke(String token) {
        redisTemplate.opsForValue().set(KEY_PREFIX + hash(token), "1", MAX_TOKEN_LIFETIME);
    }

    public boolean isRevoked(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + hash(token)));
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
