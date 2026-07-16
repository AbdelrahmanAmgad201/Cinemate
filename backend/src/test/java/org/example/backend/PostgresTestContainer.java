package org.example.backend;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Single shared PostgreSQL container for integration tests (replaces the former MySQL +
 * MongoDB + Redis containers). Flyway builds the schema from the production migrations,
 * so tests exercise the real triggers/constraints/generated columns.
 */
public class PostgresTestContainer {

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("cinemate_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        POSTGRES_CONTAINER.start();
    }

    public static String getJdbcUrl() {
        return POSTGRES_CONTAINER.getJdbcUrl();
    }

    public static String getUsername() {
        return POSTGRES_CONTAINER.getUsername();
    }

    public static String getPassword() {
        return POSTGRES_CONTAINER.getPassword();
    }
}
