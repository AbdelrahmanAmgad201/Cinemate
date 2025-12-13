package org.example.backend.admin;

import org.example.backend.movie.MovieRepository;
import org.example.backend.organization.Organization;
import org.example.backend.requests.RequestsRepository;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class AdminServiceTest {

    private MovieRepository movieRepository;
    private RequestsRepository requestsRepository;
    private AdminRepository adminRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AdminService adminService;

    @BeforeEach
    void setup() {
        movieRepository = mock(MovieRepository.class);
        requestsRepository = mock(RequestsRepository.class);
        adminRepository = mock(AdminRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        adminService = new AdminService(movieRepository, requestsRepository, adminRepository, userRepository,passwordEncoder);
    }

    @Test
    void testGetSystemOverview_WithOrganizations() {
        // Mock counts
        when(userRepository.count()).thenReturn(50L);
        when(movieRepository.countByAdminIsNotNull()).thenReturn(20L);

        // Create organization objects
        Organization org1 = Organization.builder()
                .id(3L)
                .name("Organization A")
                .email("orgA@example.com")
                .password("pass")
                .build();

        // Mock repository to return a list of organizations
        when(movieRepository.getMostPopularOrganization(PageRequest.of(0,1)))
                .thenReturn(List.of(org1));

        // Mock most rated movie
        when(movieRepository.getMostRatedMovie(PageRequest.of(0,1)))
                .thenReturn(List.of(101L));

        // Mock most liked movie
        when(movieRepository.getMostLikedMovie(PageRequest.of(0,1)))
                .thenReturn(List.of(201L));

        // Call service
        SystemOverview overview = adminService.getSystemOverview();

        // Verify
        assertEquals(50L, overview.getNumberOfUsers());
        assertEquals(20L, overview.getNumberOfMovies());
        assertEquals(3L, overview.getMostPopularOrganization().getId());
        assertEquals(101L, overview.getMostRatedMovie());
        assertEquals(201L, overview.getMostLikedMovie());
    }

    @Test
    void testGetSystemOverview_EmptyData() {
        // Mock counts
        when(userRepository.count()).thenReturn(0L);
        when(movieRepository.countByAdminIsNotNull()).thenReturn(0L);

        // Empty lists for metrics
        when(movieRepository.getMostPopularOrganization(PageRequest.of(0,1)))
                .thenReturn(Collections.emptyList());
        when(movieRepository.getMostRatedMovie(PageRequest.of(0,1)))
                .thenReturn(Collections.emptyList());
        when(movieRepository.getMostLikedMovie(PageRequest.of(0,1)))
                .thenReturn(Collections.emptyList());

        SystemOverview overview = adminService.getSystemOverview();

        assertEquals(0L, overview.getNumberOfUsers());
        assertEquals(0L, overview.getNumberOfMovies());
        assertNull(overview.getMostPopularOrganization());
        assertNull(overview.getMostRatedMovie());
        assertNull(overview.getMostLikedMovie());
    }

    @Test
    void testGetSystemOverview_PartialData() {
        // Mock counts
        when(userRepository.count()).thenReturn(5L);
        when(movieRepository.countByAdminIsNotNull()).thenReturn(10L);

        // Only most liked movie exists
        when(movieRepository.getMostPopularOrganization(PageRequest.of(0,1)))
                .thenReturn(Collections.emptyList());
        when(movieRepository.getMostRatedMovie(PageRequest.of(0,1)))
                .thenReturn(Collections.emptyList());
        when(movieRepository.getMostLikedMovie(PageRequest.of(0,1)))
                .thenReturn(List.of(77L));

        SystemOverview overview = adminService.getSystemOverview();

        assertEquals(5L, overview.getNumberOfUsers());
        assertEquals(10L, overview.getNumberOfMovies());
        assertNull(overview.getMostPopularOrganization());
        assertNull(overview.getMostRatedMovie());
        assertEquals(77L, overview.getMostLikedMovie());
    }
}
