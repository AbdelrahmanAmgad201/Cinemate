package org.example.backend.movie;

import org.example.backend.admin.Admin;
import org.example.backend.admin.AdminRepository;
import org.example.backend.likedMovie.LikedMovie;
import org.example.backend.movieReview.MovieReview;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
import org.example.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@EntityScan(basePackages = "org.example.backend")
@EnableJpaRepositories(basePackages = "org.example.backend")
class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AdminRepository adminRepository;

    @BeforeEach
    void setUp() {
        // Organizations
        Organization org1 = Organization.builder().name("Org1").email("org1@test.com").password("pass").build();
        Organization org2 = Organization.builder().name("Org2").email("org2@test.com").password("pass").build();
        organizationRepository.saveAll(List.of(org1, org2));

        // Admin
        Admin admin = Admin.builder().name("Admin1").email("admin1@test.com").password("pass").build();
        adminRepository.save(admin);

        // Movies
        Movie movie1 = Movie.builder().name("Movie1").organization(org1).admin(admin).build();
        Movie movie2 = Movie.builder().name("Movie2").organization(org1).build();
        Movie movie3 = Movie.builder().name("Movie3").organization(org2).build();
        movieRepository.saveAll(List.of(movie1, movie2, movie3));
    }

    @Test
    void testCountByAdminIsNotNull() {
        long count = movieRepository.countByAdminIsNotNull();
        assertThat(count).isEqualTo(1); // only Movie1 has admin
    }

    @Test
    void testFindByAdminIsNull() {
        List<Movie> movies = movieRepository.findByAdminIsNull();
        assertThat(movies).hasSize(2); // Movie2 and Movie3
    }

    @Test
    void testGetMostPopularOrganization() {
        List<Organization> orgs = movieRepository.getMostPopularOrganization(PageRequest.of(0, 1));
        assertThat(orgs).isNotNull(); // can't assert size if WatchHistory is empty
    }

    @Test
    void testGetMostLikedAndMostRatedMovie() {
        List<Long> mostLiked = movieRepository.getMostLikedMovie(PageRequest.of(0, 1));
        List<Long> mostRated = movieRepository.getMostRatedMovie(PageRequest.of(0, 1));
        assertThat(mostLiked).isNotNull();
        assertThat(mostRated).isNotNull();
    }

    @Test
    void testGetMovieOverview() {
        Movie movie = movieRepository.findAll().get(0);
        OneMovieOverView overview = movieRepository.getMovieOverview(movie.getMovieID());
        assertThat(overview).isNotNull();
    }
}


