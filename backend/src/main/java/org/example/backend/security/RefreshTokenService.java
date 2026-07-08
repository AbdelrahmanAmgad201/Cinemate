package org.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Server-side store for the long-lived, opaque refresh tokens that replace the
 * old access-token blacklist (SEC-01).
 *
 * <p>Design:
 * <ul>
 *   <li><b>Opaque, not a JWT.</b> A refresh token is 256 bits of randomness, not a
 *       signed claim set — it carries no readable identity, so a leaked token is
 *       useless without this store, and it can be invalidated instantly by
 *       deleting the entry (which a stateless JWT can't).</li>
 *   <li><b>Hashed at rest.</b> Only the SHA-256 of the token is the Redis key, so a
 *       Redis dump never yields usable tokens — same rationale as the verification
 *       codes (SEC-10).</li>
 *   <li><b>Single-use rotation.</b> {@link #rotate} atomically GETDELs the presented
 *       token and issues a fresh one, so every refresh invalidates its predecessor.
 *       A token that's presented twice (theft + the victim's own use) fails the
 *       second time because it's already gone — the standard rotation guarantee.</li>
 * </ul>
 *
 * <p>Storage note: this reuses the backend's existing Redis connection, which points
 * at the eviction-capable {@code redis-cache} instance. That's fine for now — losing
 * a refresh token only forces a re-login, not data loss — but the durable {@code redis}
 * instance is the more correct long-term home (a dedicated connection is a clean
 * follow-up once the gateway split lands).
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh-token:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    @Value("${jwt.refresh-token-expiration-ms:604800000}") // 7 days
    private long refreshTokenExpirationMs;

    private final StringRedisTemplate redisTemplate;

    /** Identity re-hydrated from a valid refresh token, plus the rotated replacement. */
    public record RefreshResult(String email, String role, String newRefreshToken) {}

    /**
     * Issues a new refresh token bound to the given account and returns the raw
     * token (the caller places it in the httpOnly cookie).
     */
    public String issue(String email, String role) {
        String rawToken = randomToken();
        redisTemplate.opsForValue().set(
                KEY_PREFIX + hash(rawToken),
                role + "|" + email,   // role has a fixed vocabulary with no '|', so split-on-first is safe
                Duration.ofMillis(refreshTokenExpirationMs));
        return rawToken;
    }

    /**
     * Validates and consumes a refresh token, issuing a replacement. Returns empty
     * if the token is unknown (expired, already-rotated, revoked, or forged).
     */
    public Optional<RefreshResult> rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        // GETDEL: read-and-invalidate in one atomic step so a token can only be
        // successfully rotated once, even under concurrent requests.
        String stored = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + hash(rawToken));
        if (stored == null) {
            return Optional.empty();
        }
        String[] parts = stored.split("\\|", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }
        String role = parts[0];
        String email = parts[1];
        String newToken = issue(email, role);
        return Optional.of(new RefreshResult(email, role, newToken));
    }

    /** Invalidates a refresh token (logout). No-op if it's already gone. */
    public void revoke(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            redisTemplate.delete(KEY_PREFIX + hash(rawToken));
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
