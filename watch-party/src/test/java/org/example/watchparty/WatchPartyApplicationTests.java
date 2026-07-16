package org.example.watchparty;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

// "test" profile overlays a throwaway jwt.public-key onto the base config so the context
// can build the JwtDecoder without the JWT_PUBLIC_KEY env var (see application-test.properties).
@SpringBootTest
@ActiveProfiles("test")
class WatchPartyApplicationTests {

    @MockitoBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean(name = "container")
    private RedisMessageListenerContainer container;

    @Test
    void contextLoads() {
    }

}
