package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

/**
 * Phase 2: the gateway becomes the RS256 resource server and enforces the
 * public/protected route matrix at the edge.
 *
 * <p>Tokens are verified with the RSA <b>public</b> key only (static, from env) — the
 * gateway can validate but never mint. The route→role rules mirror the backend's
 * SecurityConfig exactly; the backend still validates independently for now
 * (belt-and-suspenders) until the Phase 3 cutover.
 *
 * <p>Everything that isn't an API path (the SPA, static assets, /config.js, client-side
 * routes) is served publicly — auth only gates the API and only where the backend
 * gated it before.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight + the gateway's own health probe
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // ── Public API (mirrors backend SecurityConfig, order preserved) ──
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/verification/**").permitAll()
                        .requestMatchers("/api/user/v1/sign-up").permitAll()
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/api/movie/**").permitAll()

                        // WebSocket handshake — its own auth is tracked separately (REL-08);
                        // permit here so it keeps working exactly as today.
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
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Verifies access tokens with the RSA public key. Nimbus enforces the RS256
     * signature and the exp claim by default.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.public-key}") String publicKeyMaterial) {
        return NimbusJwtDecoder.withPublicKey(parsePublicKey(publicKeyMaterial)).build();
    }

    /**
     * Maps the token's {@code role} claim (e.g. "ROLE_USER") straight to a granted
     * authority, so the hasAuthority(...) rules above line up with the backend's.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        Converter<Jwt, Collection<GrantedAuthority>> rolesConverter = jwt -> {
            String role = jwt.getClaimAsString("role");
            return (role == null || role.isBlank())
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority(role));
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(rolesConverter);
        return converter;
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
