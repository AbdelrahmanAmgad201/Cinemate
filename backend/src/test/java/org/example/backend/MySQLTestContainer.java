package org.example.backend;

import org.testcontainers.containers.MySQLContainer;

public class MySQLTestContainer {

    private static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("cinemate_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        MYSQL_CONTAINER.start();
    }

    public static String getJdbcUrl() {
        return MYSQL_CONTAINER.getJdbcUrl();
    }

    public static String getUsername() {
        return MYSQL_CONTAINER.getUsername();
    }

    public static String getPassword() {
        return MYSQL_CONTAINER.getPassword();
    }
}
