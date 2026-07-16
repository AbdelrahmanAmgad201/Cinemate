package org.example.backend.security;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Issues and verifies short-lived <b>access</b> tokens (RS256).
 *
 * <p>Access tokens are signed with the RSA <b>private</b> key (held only by this
 * backend) and verified with the RSA <b>public</b> key. Splitting the two means a
 * verifier — most importantly the future API gateway — can validate a token with
 * the public key alone and can never mint one. This is what lets token
 * verification move to the edge without giving the edge the ability to forge
 * identities (the weakness of the previous shared-secret HS256 setup).
 *
 * <p>Access tokens are deliberately short-lived. Long-term "stay logged in" is the
 * job of the opaque refresh token ({@link RefreshTokenService}), not this JWT — so
 * there is no longer any per-request revocation list to consult, and this check is
 * pure signature math with no I/O.
 */
@Component
public class JWTProvider {

    // Keys are supplied as base64 (DER) — tolerant of full PEM armor too, so either
    // form pasted into an env var works. Private key: PKCS#8. Public key: X.509/SPKI.
    @Value("${jwt.private-key}")
    private String privateKeyMaterial;

    @Value("${jwt.public-key}")
    private String publicKeyMaterial;

    @Value("${jwt.access-token-expiration-ms:900000}") // 15 minutes
    private long accessTokenExpirationMs;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    void init() {
        this.privateKey = parsePrivateKey(privateKeyMaterial);
        this.publicKey = parsePublicKey(publicKeyMaterial);
    }

    public String generateAccessToken(Authenticatable account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", account.getId());
        claims.put("email", account.getEmail());
        claims.put("name", account.getName());
        claims.put("role", account.getRole());

        if (account.getRole().equals("ROLE_USER")) {
            claims.put("profileComplete", account.getProfileComplete() != null ? account.getProfileComplete() : true);
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(account.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(privateKey)   // RSA private key → JJWT selects RS256
                .compact();
    }

    public String getEmailFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    public Long getIdFromToken(String token) {
        return parseToken(token).get("id", Long.class);
    }

    public Boolean getProfileCompleteFromToken(String token) {
        return parseToken(token).get("profileComplete", Boolean.class);
    }

    public String getNameFromToken(String token) {
        return parseToken(token).get("name", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)   // public key → verify only, cannot sign
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Key parsing ──────────────────────────────────────────────────────────

    private static PrivateKey parsePrivateKey(String material) {
        try {
            byte[] der = Base64.getDecoder().decode(stripPemArmor(material));
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT private key (expected PKCS#8 base64/PEM)", e);
        }
    }

    private static PublicKey parsePublicKey(String material) {
        try {
            byte[] der = Base64.getDecoder().decode(stripPemArmor(material));
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT public key (expected X.509/SPKI base64/PEM)", e);
        }
    }

    // Accept either raw base64 or a full PEM block, so operators can paste whichever
    // openssl gave them without hand-stripping the header/footer and newlines.
    private static String stripPemArmor(String material) {
        return material
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
    }
}
