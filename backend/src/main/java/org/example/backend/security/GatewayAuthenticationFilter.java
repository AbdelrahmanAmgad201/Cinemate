package org.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Trusts the identity the gateway has already established and forwarded as
 * {@code X-User-*} headers, and reconstructs exactly the same SecurityContext and
 * request attributes the old {@link JWTAuthenticationFilter} produced from the JWT —
 * so {@code @PreAuthorize} and every controller ({@code request.getAttribute("userId")}
 * etc.) keep working unchanged. The backend no longer parses JWTs on the request path;
 * token verification lives entirely at the gateway now.
 *
 * <p>Trust model: these headers are only trustworthy because the gateway strips any
 * inbound copies before injecting its own, and the backend is not published on the
 * host — it is reachable only across the internal Docker network, through the gateway.
 */
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    static final String H_ID = "X-User-Id";
    static final String H_ROLE = "X-User-Role";
    static final String H_EMAIL = "X-User-Email";
    static final String H_NAME = "X-User-Name";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String role = request.getHeader(H_ROLE);
        String email = request.getHeader(H_EMAIL);

        if (role != null && !role.isBlank() && email != null && !email.isBlank()) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    List.of(new SimpleGrantedAuthority(role))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            request.setAttribute("userId", parseLong(request.getHeader(H_ID)));
            request.setAttribute("userRole", role);
            request.setAttribute("userEmail", email);
            request.setAttribute("userName", request.getHeader(H_NAME));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private static Long parseLong(String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
