package org.example.backend.verification;

import org.example.backend.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@EntityScan(basePackages = "org.example.backend")
@EnableJpaRepositories(basePackages = "org.example.backend")
class VerificationRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private VerificationRepository verificationRepository;

    @Test
    void testSaveVerification() {
        Verification v = new Verification();
        v.setEmail("user@example.com");
        v.setCode("$2a$10$hashedVerificationCodePlaceholder");
        v.setRole("USER");
        v.setCreatedAt(LocalDateTime.now());

        verificationRepository.save(v);

        var found = verificationRepository.findByEmail("user@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("$2a$10$hashedVerificationCodePlaceholder");
    }
}
