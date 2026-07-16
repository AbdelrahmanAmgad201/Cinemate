package org.example.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RefreshTokenService}.
 *
 * <p>Verifies the opaque, hashed, single-use rotation semantics (SEC-01 / SEC-10):
 * <ul>
 *   <li>Raw tokens are never stored — only their SHA-256 hash.</li>
 *   <li>Rotation deletes the presented hash and issues a replacement.</li>
 *   <li>A second rotation of the same token (race or replay) returns empty.</li>
 *   <li>Revoke is a no-op for null/blank input.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour
    private static final String EMAIL = "user@example.com";
    private static final String ROLE  = "USER";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void injectExpiration() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationMs", EXPIRATION_MS);
    }

    // -----------------------------------------------------------------------
    // issue
    // -----------------------------------------------------------------------

    @Test
    void issue_SavesTokenWithHashedValue_NotRawToken() {
        String rawToken = refreshTokenService.issue(EMAIL, ROLE);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        String storedHash = captor.getValue().getTokenHash();
        // The stored hash must be a 64-char hex string (SHA-256), not the raw token itself
        assertThat(storedHash).isNotEqualTo(rawToken);
        assertThat(storedHash).matches("[0-9a-f]{64}");
    }

    @Test
    void issue_SetsExpiryInFuture() {
        Instant before = Instant.now();

        refreshTokenService.issue(EMAIL, ROLE);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        Instant expiresAt = captor.getValue().getExpiresAt();
        assertThat(expiresAt).isAfter(before);
        assertThat(expiresAt).isAfter(Instant.now().plusMillis(EXPIRATION_MS - 5_000));
    }

    // -----------------------------------------------------------------------
    // rotate — happy path
    // -----------------------------------------------------------------------

    @Test
    void rotate_ValidToken_ReturnsRefreshResultWithNewToken() {
        // Build a known raw token and derive its hash the same way the service does
        String rawToken = "aaaabbbbccccddddeeeeffffgggghhhh"; // any non-blank string
        String hashHex = sha256Hex(rawToken);

        RefreshToken stored = RefreshToken.builder()
                .tokenHash(hashHex)
                .email(EMAIL)
                .role(ROLE)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        when(refreshTokenRepository.findById(hashHex)).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.deleteByHash(hashHex)).thenReturn(1);

        Optional<RefreshTokenService.RefreshResult> result = refreshTokenService.rotate(rawToken);

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo(EMAIL);
        assertThat(result.get().role()).isEqualTo(ROLE);
        assertThat(result.get().newRefreshToken()).isNotBlank();
        assertThat(result.get().newRefreshToken()).isNotEqualTo(rawToken);
        verify(refreshTokenRepository).deleteByHash(hashHex);
    }

    // -----------------------------------------------------------------------
    // rotate — failure paths
    // -----------------------------------------------------------------------

    @Test
    void rotate_TokenNotFound_ReturnsEmpty() {
        String rawToken = "unknowntoken";
        String hashHex = sha256Hex(rawToken);

        when(refreshTokenRepository.findById(hashHex)).thenReturn(Optional.empty());

        Optional<RefreshTokenService.RefreshResult> result = refreshTokenService.rotate(rawToken);

        assertThat(result).isEmpty();
        verify(refreshTokenRepository, never()).deleteByHash(any());
    }

    @Test
    void rotate_ExpiredToken_ReturnsEmptyAndDeletesRow() {
        String rawToken = "expiredtoken";
        String hashHex = sha256Hex(rawToken);

        RefreshToken expired = RefreshToken.builder()
                .tokenHash(hashHex)
                .email(EMAIL)
                .role(ROLE)
                .expiresAt(Instant.now().minusSeconds(1)) // already expired
                .createdAt(Instant.now().minusSeconds(7200))
                .build();

        when(refreshTokenRepository.findById(hashHex)).thenReturn(Optional.of(expired));

        Optional<RefreshTokenService.RefreshResult> result = refreshTokenService.rotate(rawToken);

        assertThat(result).isEmpty();
        // The service must purge the stale row to keep the table clean
        verify(refreshTokenRepository).deleteByHash(hashHex);
    }

    @Test
    void rotate_NullToken_ReturnsEmpty() {
        Optional<RefreshTokenService.RefreshResult> result = refreshTokenService.rotate(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void rotate_BlankToken_ReturnsEmpty() {
        Optional<RefreshTokenService.RefreshResult> result = refreshTokenService.rotate("   ");

        assertThat(result).isEmpty();
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void rotate_AlreadyRotated_ReturnsEmpty() {
        // deleteByHash returning 0 means a concurrent rotation already consumed the token.
        String rawToken = "alreadyrotated";
        String hashHex = sha256Hex(rawToken);

        RefreshToken stored = RefreshToken.builder()
                .tokenHash(hashHex)
                .email(EMAIL)
                .role(ROLE)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        when(refreshTokenRepository.findById(hashHex)).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.deleteByHash(hashHex)).thenReturn(0); // race-lost

        Optional<RefreshTokenService.RefreshResult> result = refreshTokenService.rotate(rawToken);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // revoke
    // -----------------------------------------------------------------------

    @Test
    void revoke_ValidToken_DeletesRow() {
        String rawToken = "validtoken";
        String hashHex = sha256Hex(rawToken);

        refreshTokenService.revoke(rawToken);

        verify(refreshTokenRepository).deleteByHash(hashHex);
    }

    @Test
    void revoke_NullToken_IsNoOp() {
        refreshTokenService.revoke(null);

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void revoke_BlankToken_IsNoOp() {
        refreshTokenService.revoke("  ");

        verifyNoInteractions(refreshTokenRepository);
    }

    // -----------------------------------------------------------------------
    // Helper — mirrors the private hash() method in RefreshTokenService
    // -----------------------------------------------------------------------

    private static String sha256Hex(String input) {
        try {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
