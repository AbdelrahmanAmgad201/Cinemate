package com.example.gateway.config;

import com.example.gateway.security.GatewayJwtAuthenticationFilter;
import com.example.gateway.security.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Gateway security (Phase 3): validates the access token and enforces the
 * public/protected route matrix at the edge, then forwards a trusted identity to
 * the backend.
 *
 * <p>Uses a custom {@link GatewayJwtAuthenticationFilter} rather than the strict
 * oauth2 resource server: token validation is <b>opportunistic</b> (a bad/absent
 * token isn't rejected by the filter itself — the matrix below decides), which
 * mirrors the backend's original behaviour and keeps public endpoints working when
 * the browser sends a stale token. The route→role rules match the backend exactly.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                                   RateLimitFilter rateLimitFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Write auth failures as bare statuses. Both handlers set the status
                // DIRECTLY rather than via sendError(): sendError triggers a servlet
                // dispatch to /error, which the catch-all "/** -> frontend" route would
                // then proxy, turning a 401/403 into a 200 SPA response. Directly-set
                // statuses skip that dispatch.
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, ex) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // ── Public API (mirrors backend SecurityConfig, order preserved) ──
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/verification/**").permitAll()
                        .requestMatchers("/api/user/v1/sign-up").permitAll()
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/api/movie/**").permitAll()

                        // WebSocket handshake — its own auth is tracked separately (REL-08).
                        .requestMatchers("/ws/**").permitAll()

                        // ── Role-protected API ──
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/organization/**").hasAuthority("ROLE_ORGANIZATION")
                        .requestMatchers("/api/user/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/movie-review/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/watch-history/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/liked-movie/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/watch-later/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/forum/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/comment/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/vote/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/forum-follow/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/feed/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/post/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/watch-party/**").hasAuthority("ROLE_USER")

                        // Any other API path must be authenticated (backend's anyRequest()).
                        .requestMatchers("/api/**").authenticated()

                        // Everything else is the frontend (SPA + assets) — served publicly.
                        .anyRequest().permitAll()
                )
                // Populate the SecurityContext (for the matrix) + inject X-User-* before
                // authorization runs.
                .addFilterBefore(new GatewayJwtAuthenticationFilter(jwtDecoder), AuthorizationFilter.class)
                // Rate-limit right after identity is established (so per-user keying works)
                // and before authorization — cheap rejection ahead of any real work.
                .addFilterAfter(rateLimitFilter, GatewayJwtAuthenticationFilter.class);

        return http.build();
    }

    /** Verifies access tokens with the RSA public key. Nimbus enforces RS256 + exp. */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.public-key}") String publicKeyMaterial) {
        return NimbusJwtDecoder.withPublicKey(parsePublicKey(publicKeyMaterial)).build();
    }

    // Accept base64 DER or full PEM, matching the backend's tolerant key loading.
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
