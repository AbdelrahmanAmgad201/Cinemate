package com.example.gateway.config;

import com.example.gateway.security.RateLimitFilter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Wires Bucket4j's token buckets to Redis (via Lettuce) so rate-limit counters are
 * shared across gateway replicas. Points at the same cache Redis the backend uses —
 * losing a counter on a restart just resets someone's window, which is harmless.
 */
@Configuration
public class RateLimitConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient(@Value("${redis.host:redis-cache}") String host,
                                            @Value("${redis.port:6379}") int port) {
        return RedisClient.create(RedisURI.builder().withHost(host).withPort(port).build());
    }

    @Bean
    public ProxyManager<String> rateLimitProxyManager(RedisClient client) {
        StatefulRedisConnection<String, byte[]> connection =
                client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        return LettuceBasedProxyManager.builderFor(connection)
                // Bound the Redis key TTL so idle buckets are reclaimed.
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(60)))
                .build();
    }

    @Bean
    public RateLimitFilter rateLimitFilter(ProxyManager<String> proxyManager) {
        return new RateLimitFilter(proxyManager);
    }
}
