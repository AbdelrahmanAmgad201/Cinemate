package org.example.watchparty.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Trusts the identity the gateway has already verified and forwarded as {@code X-User-*}
 * headers, exposing it to controllers as {@code userId}/{@code userName} request
 * attributes — the same contract the backend uses, so this service consumes gateway
 * identity identically now that the client reaches it directly (Stage 1 / REL-08).
 *
 * <p>Trust model: these headers are trustworthy only because the gateway strips any
 * inbound copies before injecting its own, and this service is not published on the host —
 * it is reachable solely across the internal Docker network, through the gateway. (The
 * WebSocket path can't use these headers — the token rides inside the STOMP CONNECT frame
 * instead — so it is authenticated separately by StompAuthChannelInterceptor.)
 */
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    static final String H_ID = "X-User-Id";
    static final String H_NAME = "X-User-Name";
    static final String H_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String id = request.getHeader(H_ID);
        if (id != null && !id.isBlank()) {
            request.setAttribute("userId", parseLong(id));
            request.setAttribute("userName", request.getHeader(H_NAME));
            request.setAttribute("userRole", request.getHeader(H_ROLE));
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
