package com.example.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Validates the access token (RS256, public key) and, on success, forwards the
 * caller's identity to the backend as trusted {@code X-User-*} headers.
 *
 * <p>Deliberately <b>opportunistic</b>, mirroring the backend's old
 * JWTAuthenticationFilter: a missing or invalid token is not rejected here — the
 * request simply proceeds unauthenticated and the authorization matrix decides
 * whether that's allowed. This preserves the existing behaviour where public
 * endpoints (login, refresh, logout, verification) work even when the browser
 * tacks on a stale token, while protected routes are still 401/403'd by the matrix.
 *
 * <p>The {@code X-User-*} headers are always <b>stripped from the inbound request</b>
 * first, so a client can never forge an identity; they are then re-populated only
 * from a verified token. Combined with the backend having no host port (reachable
 * only across the internal Docker network, via this gateway), that makes the headers
 * a trustworthy identity channel.
 */
public class GatewayJwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String H_ID = "X-User-Id";
    public static final String H_ROLE = "X-User-Role";
    public static final String H_EMAIL = "X-User-Email";
    public static final String H_NAME = "X-User-Name";

    private final JwtDecoder jwtDecoder;

    public GatewayJwtAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Map<String, String> userHeaders = new HashMap<>();

        String token = extractToken(request);
        if (token != null) {
            try {
                Jwt jwt = jwtDecoder.decode(token); // verifies RS256 signature + exp
                String role = jwt.getClaimAsString("role");

                List<SimpleGrantedAuthority> authorities = (role == null || role.isBlank())
                        ? List.of()
                        : List.of(new SimpleGrantedAuthority(role));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                putIfPresent(userHeaders, H_ID, jwt.getClaim("id"));
                putIfPresent(userHeaders, H_ROLE, role);
                putIfPresent(userHeaders, H_EMAIL, jwt.getClaimAsString("email"));
                putIfPresent(userHeaders, H_NAME, jwt.getClaimAsString("name"));
            } catch (JwtException e) {
                // Invalid/expired token → stay unauthenticated; the matrix rejects
                // protected routes, public ones proceed. (Same as the backend did.)
            }
        }

        filterChain.doFilter(new UserHeaderRequestWrapper(request, userHeaders), response);
    }

    private static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }

    private static void putIfPresent(Map<String, String> headers, String name, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            headers.put(name, String.valueOf(value));
        }
    }

    /**
     * Hides any inbound {@code X-User-*} headers (case-insensitive) and exposes only
     * the gateway-injected ones, so downstream sees exactly what the gateway vouches for.
     */
    private static final class UserHeaderRequestWrapper extends HttpServletRequestWrapper {

        private static final Set<String> MANAGED = Set.of(
                H_ID.toLowerCase(), H_ROLE.toLowerCase(), H_EMAIL.toLowerCase(), H_NAME.toLowerCase());

        private final Map<String, String> injected;

        UserHeaderRequestWrapper(HttpServletRequest request, Map<String, String> injected) {
            super(request);
            this.injected = injected;
        }

        private boolean isManaged(String name) {
            return name != null && MANAGED.contains(name.toLowerCase());
        }

        // Look up the injected value case-insensitively.
        private String injectedValue(String name) {
            for (Map.Entry<String, String> e : injected.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue();
                }
            }
            return null;
        }

        @Override
        public String getHeader(String name) {
            if (isManaged(name)) {
                return injectedValue(name);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isManaged(name)) {
                String value = injectedValue(name);
                return value == null ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(value));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                String name = original.nextElement();
                if (!isManaged(name)) {
                    names.add(name);
                }
            }
            names.addAll(injected.keySet());
            return Collections.enumeration(names);
        }
    }
}
