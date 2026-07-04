package org.example.watchparty.security;

import jakarta.annotation.PostConstruct;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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

    @PostConstruct
    void validateConfiguration() {
        if (expectedKey == null || expectedKey.isBlank()) {
            log.error("WATCHPARTY_KEY is not configured — all non-health requests will be rejected until it is set");
        }
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip health checks — they have no key. Exact match (+ "/api/health/" prefix
        // for sub-paths) rather than startsWith("/api/health"), which would also match
        // a future path like "/api/health-admin" and silently bypass the key check for
        // it (SEC-NEW-04) — not exploitable today (no such path exists), but a footgun
        // for the next endpoint someone adds.
        if (path.equals("/api/health") || path.startsWith("/api/health/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip the WebSocket endpoint and its SockJS sub-paths (e.g. /ws/info,
        // /ws/<server>/<session>/websocket): this is the browser-facing endpoint real
        // end users connect to directly (via the frontend's nginx proxy, not the
        // backend), so it can never carry the backend-to-microservice internal key.
        // Without this exclusion the filter's blanket "/*" coverage silently 401s
        // every real client connection — WebSocket auth is tracked separately (REL-08),
        // not provided by this filter.
        if (path.equals("/ws") || path.startsWith("/ws/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Fail closed: an unconfigured key means the service can't tell friend from foe.
        if (expectedKey == null || expectedKey.isBlank()) {
            log.error("Rejecting request to {} — WATCHPARTY_KEY is not configured", path);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"error\":\"Service misconfigured\"}");
            return;
        }

        String providedKey = request.getHeader(HEADER_NAME);
        // Constant-time comparison (SEC-NEW-03) — String.equals() short-circuits on the
        // first mismatched character, making comparison time leak how many leading
        // characters matched. Low-probability timing side channel for an internal
        // service-to-service call, but correct-by-construction is free here.
        if (!MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                (providedKey != null ? providedKey : "").getBytes(StandardCharsets.UTF_8))) {
            log.warn("Rejected request to {} — missing or invalid {}", path, HEADER_NAME);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
