package org.example.backend.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OAuthExchangeService}.
 *
 * <p>Covers the full lifecycle of a short-lived, single-use OAuth exchange code (SEC-04):
 * issuance, valid redemption, and all failure paths (already consumed, expired, bad UUID,
 * unknown code). No Spring context is needed; the repository is mocked.
 */
@ExtendWith(MockitoExtension.class)
class OAuthExchangeServiceTest {

    @Mock
    private OAuthExchangeCodeRepository codeRepository;

    @InjectMocks
    private OAuthExchangeService oAuthExchangeService;

    // -----------------------------------------------------------------------
    // issueCode
    // -----------------------------------------------------------------------

    @Test
    void issueCode_SavesEntityAndReturnsUuidString() {
        String token = "test-jwt-token";

        String result = oAuthExchangeService.issueCode(token);

        // Result must be a valid UUID string
        UUID parsed = UUID.fromString(result); // throws if not a valid UUID
        assertThat(parsed).isNotNull();

        // One entity must have been saved with the matching code and token
        ArgumentCaptor<OAuthExchangeCode> captor = ArgumentCaptor.forClass(OAuthExchangeCode.class);
        verify(codeRepository).save(captor.capture());
        OAuthExchangeCode saved = captor.getValue();

        assertThat(saved.getCode()).isEqualTo(parsed);
        assertThat(saved.getToken()).isEqualTo(token);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    // -----------------------------------------------------------------------
    // redeemCode — happy path
    // -----------------------------------------------------------------------

    @Test
    void redeemCode_ValidCode_ReturnsTokenAndDeletesRow() {
        UUID id = UUID.randomUUID();
        String expectedToken = "valid-jwt";
        OAuthExchangeCode entry = OAuthExchangeCode.builder()
                .code(id)
                .token(expectedToken)
                .expiresAt(Instant.now().plusSeconds(30))
                .build();

        when(codeRepository.findById(id)).thenReturn(Optional.of(entry));
        when(codeRepository.deleteByCode(id)).thenReturn(1);

        String result = oAuthExchangeService.redeemCode(id.toString());

        assertThat(result).isEqualTo(expectedToken);
        verify(codeRepository).deleteByCode(id);
    }

    // -----------------------------------------------------------------------
    // redeemCode — failure paths
    // -----------------------------------------------------------------------

    @Test
    void redeemCode_AlreadyConsumed_ReturnsNull() {
        // deleteByCode returning 0 means some concurrent caller already deleted the row.
        UUID id = UUID.randomUUID();
        OAuthExchangeCode entry = OAuthExchangeCode.builder()
                .code(id)
                .token("some-token")
                .expiresAt(Instant.now().plusSeconds(30))
                .build();

        when(codeRepository.findById(id)).thenReturn(Optional.of(entry));
        when(codeRepository.deleteByCode(id)).thenReturn(0); // race-lost

        String result = oAuthExchangeService.redeemCode(id.toString());

        assertThat(result).isNull();
    }

    @Test
    void redeemCode_ExpiredCode_ReturnsNull() {
        // The service checks expiry AFTER the delete succeeds (single-use invariant first).
        // An expired-but-not-yet-cleaned-up row should still return null after the delete.
        UUID id = UUID.randomUUID();
        OAuthExchangeCode expiredEntry = OAuthExchangeCode.builder()
                .code(id)
                .token("stale-token")
                .expiresAt(Instant.now().minusSeconds(60)) // already expired
                .build();

        when(codeRepository.findById(id)).thenReturn(Optional.of(expiredEntry));
        when(codeRepository.deleteByCode(id)).thenReturn(1); // delete succeeds

        String result = oAuthExchangeService.redeemCode(id.toString());

        // Token is withheld even though the row was physically deleted
        assertThat(result).isNull();
        verify(codeRepository).deleteByCode(id);
    }

    @Test
    void redeemCode_InvalidUuidString_ReturnsNull() {
        String result = oAuthExchangeService.redeemCode("not-a-uuid");

        assertThat(result).isNull();
        verifyNoInteractions(codeRepository);
    }

    @Test
    void redeemCode_UnknownCode_ReturnsNull() {
        UUID id = UUID.randomUUID();
        when(codeRepository.findById(id)).thenReturn(Optional.empty());

        String result = oAuthExchangeService.redeemCode(id.toString());

        assertThat(result).isNull();
        verify(codeRepository, never()).deleteByCode(any());
    }
}
