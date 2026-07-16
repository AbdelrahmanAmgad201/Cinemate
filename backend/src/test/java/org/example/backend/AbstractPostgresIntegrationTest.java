package org.example.backend;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for any test needing a real datasource: points Spring at a shared Testcontainers
 * PostgreSQL instance (matches the postgres:16 image in compose.yaml). Flyway builds the
 * schema from the production migrations, so tests run against the real triggers,
 * constraints, and generated columns. Replaces the former MySQL + MongoDB + Redis bases.
 */
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractPostgresIntegrationTest {

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainer::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainer::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainer::getPassword);
    }
}
