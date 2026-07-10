package org.example.backend;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for any test (full {@code @SpringBootTest} or sliced {@code @DataJpaTest})
 * that needs a real MySQL datasource. Points the app at a shared Testcontainers
 * MySQL instance instead of the auto-configured embedded database.
 *
 * <p>Also wires a Testcontainers Redis instance. Spring's cache abstraction connects
 * to Redis lazily on first use, not at context startup, so this only actually matters
 * for {@code @SpringBootTest} classes that exercise a {@code @Cacheable}/{@code @CacheEvict}
 * method (e.g. PostService.addPost) — but it's registered here, on the common base,
 * rather than per-test, so the next such test doesn't silently fail in CI with a
 * RedisConnectionFailureException the way PostServiceIntegrationTest originally did.
 */
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractMySQLIntegrationTest {

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MySQLTestContainer::getJdbcUrl);
        registry.add("spring.datasource.username", MySQLTestContainer::getUsername);
        registry.add("spring.datasource.password", MySQLTestContainer::getPassword);
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", RedisTestContainer::getHost);
        registry.add("spring.data.redis.port", RedisTestContainer::getPort);
    }
}
