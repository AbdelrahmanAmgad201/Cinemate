package org.example.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the httpOnly cookie that carries the refresh token.
 *
 * <p>Why a cookie and not the response body: an httpOnly cookie is unreadable from
 * JavaScript, so an XSS bug can't exfiltrate the refresh token (the long-lived
 * credential). The short-lived access token still travels in the body/Authorization
 * header as before.
 *
 * <p>Scope is {@code /api/auth/v1} so the browser attaches it to exactly two
 * endpoints that need it — {@code /refresh} and {@code /logout} — and nowhere else.
 *
 * <p>{@code SameSite=None; Secure} is required today because the frontend
 * ({@code :5173}) and backend ({@code :8080}) are separate origins. Browsers treat
 * {@code localhost} as a secure context, so {@code Secure} works over http in dev.
 * Once the gateway puts everything behind one origin, tighten this to
 * {@code SameSite=Lax} (or {@code Strict}) via the {@code app.cookie.same-site} property.
 */
@Component
public class RefreshTokenCookie {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String PATH = "/api/auth/v1";

    @Value("${app.cookie.secure:true}")
    private boolean secure;

    @Value("${app.cookie.same-site:None}")
    private String sameSite;

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    /** Cookie carrying a freshly issued/rotated refresh token. */
    public ResponseCookie build(String rawToken) {
        return baseBuilder(rawToken)
                .maxAge(Duration.ofMillis(refreshTokenExpirationMs))
                .build();
    }

    /** Expired empty cookie that clears the refresh token on logout. */
    public ResponseCookie clear() {
        return baseBuilder("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(PATH);
    }
}
