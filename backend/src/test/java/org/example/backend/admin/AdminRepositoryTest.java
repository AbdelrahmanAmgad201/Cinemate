package org.example.backend.admin;

import org.example.backend.movie.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
@EntityScan(basePackages = "org.example.backend")
@EnableJpaRepositories(basePackages = "org.example.backend")
class AdminRepositoryTest {

    @Autowired
    private AdminRepository adminRepository;

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = Admin.builder()
                .name("Super Admin")
                .email("admin@example.com")
                .password("adminpass")
                .build();
    }

    @Test
    void testSaveAndFindAdmin() {
        adminRepository.save(admin);

        var found = adminRepository.findByEmail("admin@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("admin@example.com");
        assertThat(found.get().getName()).isEqualTo("Super Admin");
    }

    @Test
    void testUpdateAdminName() {
        Admin saved = adminRepository.save(admin);

        saved.setName("Updated Admin");
        adminRepository.save(saved);

        var found = adminRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Updated Admin");
    }

    @Test
    void testDeleteAdmin() {
        Admin saved = adminRepository.save(admin);
        Long id = saved.getId();

        adminRepository.delete(saved);

        assertThat(adminRepository.findById(id)).isNotPresent();
    }

    @Test
    void testUniqueEmailConstraint() {
        adminRepository.save(admin);

        Admin duplicate = Admin.builder()
                .name("Another Admin")
                .email("admin@example.com") // same email
                .password("pass")
                .build();

        assertThrows(Exception.class, () -> adminRepository.saveAndFlush(duplicate));
    }

    @Test
    void testApprovedMoviesAssociation() {
        Movie movie1 = Movie.builder().name("Movie 1").build();
        Movie movie2 = Movie.builder().name("Movie 2").build();

        admin.setApprovedMovies(List.of(movie1, movie2));
        Admin saved = adminRepository.save(admin);

        var found = adminRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getApprovedMovies()).hasSize(2);
        assertThat(found.getApprovedMovies().get(0).getName()).isEqualTo("Movie 1");
        assertThat(found.getApprovedMovies().get(1).getName()).isEqualTo("Movie 2");
    }

    @Test
    void testGetRole() {
        assertThat(admin.getRole()).isEqualTo("ROLE_ADMIN");
    }
}
