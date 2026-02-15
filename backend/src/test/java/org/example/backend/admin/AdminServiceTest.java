package org.example.backend.admin;

import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.organization.Organization;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsRepository;
import org.example.backend.requests.State;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

        adminService = new AdminService(movieRepository, requestsRepository,
                adminRepository, userRepository, passwordEncoder);
    }

    // =============== GetRequestedMovie Tests ===============
    @Test
    void testGetRequestedMovie_Success() {
        Long requestId = 1L;
        Movie movie = new Movie();
        movie.setMovieID(100L);
        movie.setName("Test Movie");

        Requests request = new Requests();
        request.setId(requestId);
        request.setMovie(movie);

        when(requestsRepository.findById(requestId)).thenReturn(Optional.of(request));

        Movie result = adminService.getRequestedMovie(requestId);

        assertNotNull(result);
        assertEquals(100L, result.getMovieID());
        assertEquals("Test Movie", result.getName());
        verify(requestsRepository).findById(requestId);
    }

    @Test
    void testGetRequestedMovie_RequestNotFound() {
        Long requestId = 999L;
        when(requestsRepository.findById(requestId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.getRequestedMovie(requestId));

        assertEquals("Request not found", exception.getMessage());
        verify(requestsRepository).findById(requestId);
    }

    // =============== AcceptRequests Tests ===============
    @Test
    void testAcceptRequests_Success() {
        Long adminId = 1L;
        Long requestId = 2L;

        Admin admin = new Admin();
        admin.setId(adminId);
        admin.setName("Admin User");

        Movie movie = new Movie();
        movie.setMovieID(100L);
        movie.setName("Test Movie");

        Requests request = new Requests();
        request.setId(requestId);
        request.setMovie(movie);
        request.setState(State.PENDING);

        when(adminRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(requestsRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(requestsRepository.save(any(Requests.class))).thenReturn(request);
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);

        adminService.acceptRequests(adminId, requestId);

        // Verify request state was updated
        assertEquals(State.ACCEPTED, request.getState());
        assertEquals(admin, request.getAdmin());
        assertNotNull(request.getStateUpdatedAt());

        // Verify movie admin was set
        assertEquals(admin, movie.getAdmin());

        // Verify saves were called
        verify(requestsRepository).save(request);
        verify(movieRepository).save(movie);
    }

    @Test
    void testAcceptRequests_AdminNotFound() {
        Long adminId = 999L;
        Long requestId = 2L;

        when(adminRepository.findById(adminId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.acceptRequests(adminId, requestId));

        assertEquals("Admin not found", exception.getMessage());
        verify(adminRepository).findById(adminId);
        verifyNoInteractions(requestsRepository);
        verifyNoInteractions(movieRepository);
    }

    @Test
    void testAcceptRequests_RequestNotFound() {
        Long adminId = 1L;
        Long requestId = 999L;

        Admin admin = new Admin();
        admin.setId(adminId);

        when(adminRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(requestsRepository.findById(requestId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.acceptRequests(adminId, requestId));

        assertEquals("Movie not found", exception.getMessage());
        verify(requestsRepository).findById(requestId);
        verify(movieRepository, never()).save(any());
    }

    @Test
    void testAcceptRequests_StateUpdatedAtIsSet() {
        Long adminId = 1L;
        Long requestId = 2L;

        Admin admin = new Admin();
        Movie movie = new Movie();
        Requests request = new Requests();
        request.setMovie(movie);

        when(adminRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(requestsRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(requestsRepository.save(any(Requests.class))).thenReturn(request);
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);

        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);
        adminService.acceptRequests(adminId, requestId);
        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);

        assertNotNull(request.getStateUpdatedAt());
        assertTrue(request.getStateUpdatedAt().isAfter(beforeCall));
        assertTrue(request.getStateUpdatedAt().isBefore(afterCall));
    }

    // =============== DeclineRequest Tests ===============
    @Test
    void testDeclineRequest_Success() {
        Long adminId = 1L;
        Long requestId = 2L;
        Long movieId = 100L;

        Admin admin = new Admin();
        admin.setId(adminId);

        Movie movie = new Movie();
        movie.setMovieID(movieId);

        Requests request = new Requests();
        request.setId(requestId);
        request.setMovie(movie);
        request.setState(State.PENDING);

        when(requestsRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(adminRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(requestsRepository.save(any(Requests.class))).thenReturn(request);
        doNothing().when(movieRepository).deleteById(movieId);

        adminService.declineRequest(adminId, requestId);

        // Verify request state was updated
        assertEquals(State.REJECTED, request.getState());
        assertEquals(admin, request.getAdmin());
        assertNull(request.getMovie());
        assertNotNull(request.getStateUpdatedAt());

        // Verify movie was deleted
        verify(movieRepository).deleteById(movieId);
        verify(requestsRepository).save(request);
    }

    @Test
    void testDeclineRequest_RequestNotFound() {
        Long adminId = 1L;
        Long requestId = 999L;

        when(requestsRepository.findById(requestId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.declineRequest(adminId, requestId));

        assertEquals("Movie not found", exception.getMessage());
        verify(requestsRepository).findById(requestId);
        verifyNoInteractions(adminRepository);
        verifyNoInteractions(movieRepository);
    }

    @Test
    void testDeclineRequest_AdminNotFound() {
        Long adminId = 999L;
        Long requestId = 2L;

        Movie movie = new Movie();
        movie.setMovieID(100L);

        Requests request = new Requests();
        request.setMovie(movie);

        when(requestsRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(adminRepository.findById(adminId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.declineRequest(adminId, requestId));

        assertEquals("Admin not found", exception.getMessage());
        verify(adminRepository).findById(adminId);
        verify(movieRepository, never()).deleteById(any());
    }

    @Test
    void testDeclineRequest_StateUpdatedAtIsSet() {
        Long adminId = 1L;
        Long requestId = 2L;

        Admin admin = new Admin();
        Movie movie = new Movie();
        movie.setMovieID(100L);

        Requests request = new Requests();
        request.setMovie(movie);

        when(requestsRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(adminRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(requestsRepository.save(any(Requests.class))).thenReturn(request);

        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);
        adminService.declineRequest(adminId, requestId);
        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);

        assertNotNull(request.getStateUpdatedAt());
        assertTrue(request.getStateUpdatedAt().isAfter(beforeCall));
        assertTrue(request.getStateUpdatedAt().isBefore(afterCall));
    }

    // =============== AddAdmin Tests ===============
    @Test
    void testAddAdmin_Success() {
        AddAdminDTO dto = new AddAdminDTO();
        dto.setName("New Admin");
        dto.setEmail("admin@example.com");
        dto.setPassword("plainPassword");

        String encodedPassword = "encodedPassword123";
        when(passwordEncoder.encode("plainPassword")).thenReturn(encodedPassword);

        Admin savedAdmin = Admin.builder()
                .id(1L)
                .name("New Admin")
                .email("admin@example.com")
                .password(encodedPassword)
                .build();

        when(adminRepository.save(any(Admin.class))).thenReturn(savedAdmin);

        Admin result = adminService.addAdmin(dto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("New Admin", result.getName());
        assertEquals("admin@example.com", result.getEmail());
        assertEquals(encodedPassword, result.getPassword());

        // Verify password was encoded
        verify(passwordEncoder).encode("plainPassword");

        // Verify save was called with correct data
        ArgumentCaptor<Admin> adminCaptor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(adminCaptor.capture());
        Admin capturedAdmin = adminCaptor.getValue();
        assertEquals("New Admin", capturedAdmin.getName());
        assertEquals("admin@example.com", capturedAdmin.getEmail());
        assertEquals(encodedPassword, capturedAdmin.getPassword());
    }

    @Test
    void testAddAdmin_PasswordIsEncoded() {
        AddAdminDTO dto = new AddAdminDTO();
        dto.setName("Admin");
        dto.setEmail("test@test.com");
        dto.setPassword("password123");

        when(passwordEncoder.encode("password123")).thenReturn("encoded_password123");
        when(adminRepository.save(any(Admin.class))).thenReturn(new Admin());

        adminService.addAdmin(dto);

        verify(passwordEncoder).encode("password123");

        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(captor.capture());
        assertEquals("encoded_password123", captor.getValue().getPassword());
    }

    // =============== GetSystemOverview Tests ===============
    @Test
    void testGetSystemOverview_WithAllData() {
        when(userRepository.count()).thenReturn(50L);
        when(movieRepository.countByAdminIsNotNull()).thenReturn(20L);

        Organization org = Organization.builder()
                .id(3L)
                .name("Organization A")
                .email("orgA@example.com")
                .password("pass")
                .build();

        when(movieRepository.getMostPopularOrganization(PageRequest.of(0,1)))
                .thenReturn(List.of(org));
        when(movieRepository.getMostRatedMovie(PageRequest.of(0,1)))
                .thenReturn(List.of(101L));
        when(movieRepository.getMostLikedMovie(PageRequest.of(0,1)))
                .thenReturn(List.of(201L));

        SystemOverview overview = adminService.getSystemOverview();

        assertEquals(50L, overview.getNumberOfUsers());
        assertEquals(20L, overview.getNumberOfMovies());
        assertEquals(3L, overview.getMostPopularOrganization().getId());
        assertEquals("Organization A", overview.getMostPopularOrganization().getName());
        assertEquals(101L, overview.getMostRatedMovie());
        assertEquals(201L, overview.getMostLikedMovie());
    }

    @Test
    void testGetSystemOverview_EmptyData() {
        when(userRepository.count()).thenReturn(0L);
        when(movieRepository.countByAdminIsNotNull()).thenReturn(0L);
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
        when(userRepository.count()).thenReturn(5L);
        when(movieRepository.countByAdminIsNotNull()).thenReturn(10L);
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

    @Test
    void testGetSystemOverview_OnlyOrganizationData() {
        when(userRepository.count()).thenReturn(10L);
        when(movieRepository.countByAdminIsNotNull()).thenReturn(5L);

        Organization org = Organization.builder()
                .id(99L)
                .name("Popular Org")
                .build();

        when(movieRepository.getMostPopularOrganization(PageRequest.of(0,1)))
                .thenReturn(List.of(org));
        when(movieRepository.getMostRatedMovie(PageRequest.of(0,1)))
                .thenReturn(Collections.emptyList());
        when(movieRepository.getMostLikedMovie(PageRequest.of(0,1)))
                .thenReturn(Collections.emptyList());

        SystemOverview overview = adminService.getSystemOverview();

        assertEquals(10L, overview.getNumberOfUsers());
        assertEquals(5L, overview.getNumberOfMovies());
        assertNotNull(overview.getMostPopularOrganization());
        assertEquals(99L, overview.getMostPopularOrganization().getId());
        assertNull(overview.getMostRatedMovie());
        assertNull(overview.getMostLikedMovie());
    }

    // =============== GetAdminProfile Tests ===============
    @Test
    void testGetAdminProfile_Success() {
        Long adminId = 1L;
        Admin admin = new Admin();
        admin.setId(adminId);
        admin.setName("John Doe");
        admin.setEmail("john@example.com");

        when(adminRepository.findById(adminId)).thenReturn(Optional.of(admin));

        AdminProfileDTO result = adminService.getAdminProfile(adminId);

        assertNotNull(result);
        assertEquals("John Doe", result.name());
        assertEquals("john@example.com", result.email());
        assertEquals("ROLE_ADMIN", result.role());
        verify(adminRepository).findById(adminId);
    }

    @Test
    void testGetAdminProfile_AdminNotFound() {
        Long adminId = 999L;
        when(adminRepository.findById(adminId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.getAdminProfile(adminId));

        assertEquals("Admin not found", exception.getMessage());
        verify(adminRepository).findById(adminId);
    }

    // =============== UpdateAdminName Tests ===============
    @Test
    void testUpdateAdminName_Success() {
        Long adminId = 1L;
        String newName = "Updated Name";

        Admin admin = new Admin();
        admin.setId(adminId);
        admin.setName("Old Name");
        admin.setEmail("test@test.com");

        when(adminRepository.findById(adminId)).thenReturn(Optional.of(admin));

        adminService.updateAdminName(adminId, newName);

        assertEquals("Updated Name", admin.getName());
        verify(adminRepository).findById(adminId);
    }

    @Test
    void testUpdateAdminName_AdminNotFound() {
        Long adminId = 999L;
        String newName = "New Name";

        when(adminRepository.findById(adminId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.updateAdminName(adminId, newName));

        assertEquals("Admin not found", exception.getMessage());
        verify(adminRepository).findById(adminId);
    }

    @Test
    void testUpdateAdminName_WithEmptyString() {
        Long adminId = 1L;

        Admin admin = new Admin();
        admin.setId(adminId);
        admin.setName("Original Name");

        when(adminRepository.findById(adminId)).thenReturn(Optional.of(admin));

        adminService.updateAdminName(adminId, "");

        assertEquals("", admin.getName());
    }

    @Test
    void testUpdateAdminName_WithNull() {
        Long adminId = 1L;

        Admin admin = new Admin();
        admin.setId(adminId);
        admin.setName("Original Name");

        when(adminRepository.findById(adminId)).thenReturn(Optional.of(admin));

        adminService.updateAdminName(adminId, null);

        assertNull(admin.getName());
    }
}