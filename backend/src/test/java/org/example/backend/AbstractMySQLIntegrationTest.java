package org.example.backend;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for any test (full {@code @SpringBootTest} or sliced {@code @DataJpaTest})
 * that needs a real MySQL datasource. Points the app at a shared Testcontainers
 * MySQL instance instead of the auto-configured embedded database.
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
}
