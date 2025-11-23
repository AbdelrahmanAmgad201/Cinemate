package org.example.backend.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;


import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@EntityScan(basePackages = "org.example.backend")
@EnableJpaRepositories(basePackages = "org.example.backend")
class AdminRepositoryTest {

    @Autowired
    private AdminRepository adminRepository;

    @Test
    void testSaveAndFindAdmin() {
        Admin admin = Admin.builder()
                .name("Super Admin")
                .email("admin@example.com")
                .password("adminpass")
                .build();

        adminRepository.save(admin);

        var found = adminRepository.findByEmail("admin@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("admin@example.com");
        assertThat(found.get().getName()).isEqualTo("Super Admin"); // verify name too
    }
}
