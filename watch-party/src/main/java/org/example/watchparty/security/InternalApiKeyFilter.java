package org.example.watchparty.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates the X-Internal-API-Key header on all non-health requests.
 * This prevents direct access to the watch-party REST API from outside the Docker network.
 */
@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Internal-API-Key";

    @Value("${internal.api.key:}")
    private String expectedKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip health checks — they have no key
        if (path.startsWith("/api/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // If no key is configured, skip validation (fail-open for local dev without WATCHPARTY_KEY)
        if (expectedKey == null || expectedKey.isBlank()) {
            log.warn("WATCHPARTY_KEY is not configured — internal API key validation is disabled");
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(HEADER_NAME);
        if (!expectedKey.equals(providedKey)) {
            log.warn("Rejected request to {} — missing or invalid {}", path, HEADER_NAME);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
