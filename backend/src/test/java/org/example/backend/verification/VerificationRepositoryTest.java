package org.example.backend.verification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test") // <--- make sure this matches your test properties
@EntityScan(basePackages = "org.example.backend")
@EnableJpaRepositories(basePackages = "org.example.backend")
class VerificationRepositoryTest {

    @Autowired
    private VerificationRepository verificationRepository;

    @Test
    void testSaveVerification() {
        Verfication v = new Verfication();
        v.setEmail("user@example.com");
        v.setCode(123456);
        v.setRole("USER");
        v.setCreatedAt(LocalDateTime.now());

        verificationRepository.save(v);

        var found = verificationRepository.findByEmail("user@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo(123456);
    }
}
