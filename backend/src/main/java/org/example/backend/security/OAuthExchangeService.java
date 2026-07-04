package org.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Short-lived, single-use exchange codes for the OAuth redirect handoff.
 * Keeps the JWT out of the URL (browser history, access logs, Referer headers) —
 * see SEC-04. The code buys just enough time for the frontend's redirect page
 * to call back and trade it for the real token.
 *
 * Backed by Redis (the same "redis-cache" instance as the response cache — see
 * ARC-06) rather than the in-process Caffeine cache this used to be: the OAuth
 * redirect and the frontend's exchange-code callback can land on two different
 * backend replicas behind a load balancer, so the code has to be readable from
 * wherever it was issued. Redis's atomic GETDEL also closes a small race the
 * Caffeine version had (separate get-then-invalidate calls, so two concurrent
 * redeem attempts could both succeed) — {@code getAndDelete} does both in one
 * atomic step, so a code can only ever be redeemed once.
 */
@Service
@RequiredArgsConstructor
public class OAuthExchangeService {

    private static final Duration CODE_TTL = Duration.ofSeconds(30);
    private static final String KEY_PREFIX = "oauth-exchange:";

    private final StringRedisTemplate redisTemplate;

    public String issueCode(String token) {
        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + code, token, CODE_TTL);
        return code;
    }

    public String redeemCode(String code) {
        return redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code);
    }
}
