package org.example.watchparty.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Verifies access tokens presented on the WebSocket CONNECT frame (REL-08) with the
 * RSA public key — the same key, loaded the same tolerant way, as the gateway
 * ({@code com.example.gateway.config.SecurityConfig}). This service can validate a
 * token but, holding only the public half, can never mint one.
 *
 * <p>Deliberately fail-closed: with no {@code jwt.public-key} configured the decoder
 * bean can't be built and the service refuses to start, rather than booting with
 * WebSocket authentication silently disabled.
 */
@Configuration
public class JwtConfig {

    /** Nimbus enforces RS256 + expiry. */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.public-key}") String publicKeyMaterial) {
        return NimbusJwtDecoder.withPublicKey(parsePublicKey(publicKeyMaterial)).build();
    }

    // Accept base64 DER or full PEM, matching how the gateway and backend load the key.
    private static RSAPublicKey parsePublicKey(String material) {
        try {
            String base64 = material
                    .replaceAll("-----BEGIN [A-Z ]+-----", "")
                    .replaceAll("-----END [A-Z ]+-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT public key (expected X.509/SPKI base64/PEM)", e);
        }
    }
}
