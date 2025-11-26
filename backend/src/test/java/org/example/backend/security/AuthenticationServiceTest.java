package org.example.backend.security;

import org.example.backend.admin.Admin;
import org.example.backend.admin.AdminRepository;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authService;

    private final String email = "test@example.com";
    private final String rawPassword = "password";
    private final String encodedPassword = "encodedPassword";

    private User user;
    private Admin admin;
    private Organization organization;

    @BeforeEach
    void setup() {
        user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);

        admin = new Admin();
        admin.setEmail(email);
        admin.setPassword(encodedPassword);

        organization = new Organization();
        organization.setEmail(email);
        organization.setPassword(encodedPassword);
    }

    // -------------------------------------------------------------------------
    // TEST: findByEmailAndRole → ROLE_USER
    // -------------------------------------------------------------------------
    @Test
    void testFindByEmailAndRoleUser() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<Authenticatable> result = authService.findByEmailAndRole(email, "USER");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    // -------------------------------------------------------------------------
    // TEST: findByEmailAndRole → ROLE_ADMIN
    // -------------------------------------------------------------------------
    @Test
    void testFindByEmailAndRoleAdmin() {
        when(adminRepository.findByEmail(email)).thenReturn(Optional.of(admin));

        Optional<Authenticatable> result = authService.findByEmailAndRole(email, "ADMIN");

        assertTrue(result.isPresent());
        assertEquals(admin, result.get());
    }

    // -------------------------------------------------------------------------
    // TEST: findByEmailAndRole → ROLE_ORGANIZATION
    // -------------------------------------------------------------------------
    @Test
    void testFindByEmailAndRoleOrganization() {
        when(organizationRepository.findByEmail(email)).thenReturn(Optional.of(organization));

        Optional<Authenticatable> result = authService.findByEmailAndRole(email, "ORGANIZATION");

        assertTrue(result.isPresent());
        assertEquals(organization, result.get());
    }

    // -------------------------------------------------------------------------
    // TEST: findByEmailAndRole → unknown role
    // -------------------------------------------------------------------------
    @Test
    void testFindByEmailAndRoleUnknown() {
        Optional<Authenticatable> result = authService.findByEmailAndRole(email, "UNKNOWN");
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // TEST: authenticate → success
    // -------------------------------------------------------------------------
    @Test
    void testAuthenticateSuccess() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        Optional<Authenticatable> result = authService.authenticate(email, rawPassword, "USER");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    // -------------------------------------------------------------------------
    // TEST: authenticate → wrong password
    // -------------------------------------------------------------------------
    @Test
    void testAuthenticateWrongPassword() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        Optional<Authenticatable> result = authService.authenticate(email, rawPassword, "USER");

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // TEST: authenticate → unknown role
    // -------------------------------------------------------------------------
    @Test
    void testAuthenticateUnknownRole() {
        Optional<Authenticatable> result = authService.authenticate(email, rawPassword, "UNKNOWN");
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // TEST: normalizeRole → adds ROLE_ prefix if missing
    // -------------------------------------------------------------------------
    @Test
    void testNormalizeRoleAddsPrefix() {
        // Using reflection to invoke private method (optional)
        try {
            var method = AuthenticationService.class.getDeclaredMethod("normalizeRole", String.class);
            method.setAccessible(true);
            assertEquals("ROLE_ADMIN", method.invoke(authService, "ADMIN"));
            assertEquals("ROLE_USER", method.invoke(authService, "user"));
            assertEquals("ROLE_ORGANIZATION", method.invoke(authService, "organization"));
        } catch (Exception e) {
            fail(e);
        }
    }

    // -------------------------------------------------------------------------
    // TEST: normalizeRole → keeps ROLE_ if present
    // -------------------------------------------------------------------------
    @Test
    void testNormalizeRoleKeepsPrefix() {
        try {
            var method = AuthenticationService.class.getDeclaredMethod("normalizeRole", String.class);
            method.setAccessible(true);
            assertEquals("ROLE_ADMIN", method.invoke(authService, "ROLE_ADMIN"));
        } catch (Exception e) {
            fail(e);
        }
    }
}
