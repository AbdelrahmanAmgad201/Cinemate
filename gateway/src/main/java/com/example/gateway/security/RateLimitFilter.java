package com.example.gateway.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Token-bucket rate limiting at the edge (Bucket4j), sharing counters across gateway
 * replicas via Redis.
 *
 * <p>Keying is a per-user / per-IP hybrid:
 * <ul>
 *   <li>{@code /api/auth/**} — a tight bucket keyed by client IP. These are pre-login
 *       endpoints (login/refresh/etc.), so there's no user yet, and a small limit
 *       blunts credential-stuffing and refresh-token hammering.</li>
 *   <li>other {@code /api/**} — a larger bucket keyed by the authenticated user id
 *       (from the gateway-verified {@code X-User-Id}) when present, else the client IP.</li>
 * </ul>
 * Only {@code /api/**} is limited; the SPA and its static assets are not.
 *
 * <p>Runs right after {@link GatewayJwtAuthenticationFilter}, so the verified identity
 * is already available, and before authorization — limiting cheaply, before any real work.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;

    // Auth endpoints: capacity 10, refilling 5/sec. Everything else: capacity 40,
    // refilling 20/sec. Numbers are intentionally modest and easy to tune.
    private final Supplier<BucketConfiguration> authConfig =
            bucket(10, 5, Duration.ofSeconds(1));
    private final Supplier<BucketConfiguration> apiConfig =
            bucket(40, 20, Duration.ofSeconds(1));

    public RateLimitFilter(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response); // SPA + static assets aren't limited
            return;
        }

        String key;
        Supplier<BucketConfiguration> config;
        if (path.startsWith("/api/auth/")) {
            key = "rl:auth:ip:" + clientIp(request);
            config = authConfig;
        } else {
            String userId = request.getHeader(GatewayJwtAuthenticationFilter.H_ID);
            if (userId != null && !userId.isBlank()) {
                key = "rl:user:" + userId;
            } else {
                key = "rl:ip:" + clientIp(request);
            }
            config = apiConfig;
        }

        BucketProxy bucket = proxyManager.builder().build(key, config);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long retryAfter = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            response.setStatus(429); // 429 Too Many Requests
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests\"}");
        }
    }

    private static Supplier<BucketConfiguration> bucket(long capacity, long refillTokens, Duration period) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(refillTokens, period)
                        .build())
                .build();
        return () -> configuration;
    }

    // Behind a real proxy/LB you'd trust X-Forwarded-For; here the gateway is the edge,
    // so fall back to the socket address.
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
