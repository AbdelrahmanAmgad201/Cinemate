package org.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: verifies the full Spring ApplicationContext loads without errors.
 *
 * <p>Requires a running Postgres instance (provided by {@link PostgresTestContainer} via
 * {@link AbstractPostgresIntegrationTest}) because the context validates Flyway migrations
 * and JPA entity/schema alignment on startup.
 *
 * <p>Previously this class had no {@code @SpringBootTest} annotation and therefore loaded
 * zero beans, making the test pass vacuously. Fixed to match the pattern established by
 * {@code VerificationServiceTest}.
 */
@SpringBootTest
class BackendApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    void contextLoads() {
        // Passes when the full ApplicationContext starts without throwing an exception.
    }
}
