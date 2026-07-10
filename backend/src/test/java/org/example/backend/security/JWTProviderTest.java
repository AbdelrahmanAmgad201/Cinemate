package org.example.backend.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JWTProviderTest {

    private JWTProvider jwtProvider;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        jwtProvider = new JWTProvider();
        ReflectionTestUtils.setField(jwtProvider, "privateKeyMaterial", encode(keyPair.getPrivate().getEncoded()));
        ReflectionTestUtils.setField(jwtProvider, "publicKeyMaterial", encode(keyPair.getPublic().getEncoded()));
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpirationMs", 900_000L);
        jwtProvider.init();
    }

    private static String encode(byte[] der) {
        return Base64.getEncoder().encodeToString(der);
    }

    private Authenticatable user(String role) {
        Authenticatable account = mock(Authenticatable.class);
        when(account.getId()).thenReturn(42L);
        when(account.getEmail()).thenReturn("jane@example.com");
        when(account.getName()).thenReturn("Jane");
        when(account.getRole()).thenReturn(role);
        when(account.getProfileComplete()).thenReturn(null);
        return account;
    }

    @Test
    void generateAccessToken_ThenParse_RoundTripsAllClaims() {
        Authenticatable account = user("ROLE_USER");

        String token = jwtProvider.generateAccessToken(account);

        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getEmailFromToken(token)).isEqualTo("jane@example.com");
        assertThat(jwtProvider.getRoleFromToken(token)).isEqualTo("ROLE_USER");
        assertThat(jwtProvider.getIdFromToken(token)).isEqualTo(42L);
        assertThat(jwtProvider.getNameFromToken(token)).isEqualTo("Jane");
    }

    @Test
    void generateAccessToken_UserWithNullProfileComplete_DefaultsToTrue() {
        Authenticatable account = user("ROLE_USER");

        String token = jwtProvider.generateAccessToken(account);

        assertThat(jwtProvider.getProfileCompleteFromToken(token)).isTrue();
    }

    @Test
    void generateAccessToken_NonUserRole_OmitsProfileCompleteClaim() {
        Authenticatable account = user("ROLE_ADMIN");

        String token = jwtProvider.generateAccessToken(account);

        assertThat(jwtProvider.getProfileCompleteFromToken(token)).isNull();
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpirationMs", -1_000L);
        String token = jwtProvider.generateAccessToken(user("ROLE_USER"));

        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_TamperedSignature_ReturnsFalse() {
        String token = jwtProvider.generateAccessToken(user("ROLE_USER"));
        String[] parts = token.split("\\.");
        String signature = parts[2];
        // Flip a character in the middle of the signature rather than the last one:
        // trailing base64url characters can include "don't care" padding bits that
        // decode to the same byte, which would make the tamper a no-op.
        int mid = signature.length() / 2;
        char flipped = signature.charAt(mid) == 'a' ? 'b' : 'a';
        String tamperedSignature = signature.substring(0, mid) + flipped + signature.substring(mid + 1);
        String tampered = parts[0] + "." + parts[1] + "." + tamperedSignature;

        assertThat(jwtProvider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_SignedWithDifferentKey_ReturnsFalse() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair otherKeyPair = generator.generateKeyPair();

        String foreignToken = Jwts.builder()
                .subject("attacker@example.com")
                .claim("role", "ROLE_ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(otherKeyPair.getPrivate())
                .compact();

        assertThat(jwtProvider.validateToken(foreignToken)).isFalse();
    }

    @Test
    void validateToken_MalformedString_ReturnsFalse() {
        assertThat(jwtProvider.validateToken("not-a-jwt")).isFalse();
        assertThat(jwtProvider.validateToken("")).isFalse();
    }

    @Test
    void getRoleFromToken_TokenMissingClaim_ReturnsNullRatherThanThrowing() {
        String token = Jwts.builder()
                .subject("no-role@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(keyPair.getPrivate())
                .compact();

        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getRoleFromToken(token)).isNull();
        assertThat(jwtProvider.getIdFromToken(token)).isNull();
    }
}
