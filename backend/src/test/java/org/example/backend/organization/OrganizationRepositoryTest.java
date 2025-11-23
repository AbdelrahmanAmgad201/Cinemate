package org.example.backend.organization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") // Make sure this matches your test properties
@EntityScan(basePackages = "org.example.backend")
@EnableJpaRepositories(basePackages = "org.example.backend")
class OrganizationRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void testSaveAndFindOrganization() {
        Organization organization = Organization.builder()
                .email("org@example.com")
                .password("password123")
                .name("Test Organization")
                .createdAt(LocalDateTime.now())
                .build();

        organizationRepository.save(organization);

        var found = organizationRepository.findByEmail("org@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("org@example.com");
        assertThat(found.get().getName()).isEqualTo("Test Organization");
    }
}
