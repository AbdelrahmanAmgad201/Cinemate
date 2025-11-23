package org.example.backend.security;

import org.example.backend.admin.Admin;
import org.example.backend.admin.AdminRepository;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    private UserRepository userRepository;
    private AdminRepository adminRepository;
    private OrganizationRepository organizationRepository;
    private PasswordEncoder passwordEncoder;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        adminRepository = mock(AdminRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        authenticationService = new AuthenticationService(
                userRepository,
                adminRepository,
                organizationRepository,
                passwordEncoder
        );
    }

    // -------------------------------------------------------
    // TEST: findByEmailAndRole
    // -------------------------------------------------------

    @Test
    void testFindByEmailAndRole_User() {
        User user = new User();
        user.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        var result = authenticationService.findByEmailAndRole("user@example.com", "USER");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void testFindByEmailAndRole_Admin() {
        Admin admin = new Admin();
        admin.setEmail("admin@example.com");

        when(adminRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));

        var result = authenticationService.findByEmailAndRole("admin@example.com", "ADMIN");

        assertTrue(result.isPresent());
        assertEquals(admin, result.get());
    }

    @Test
    void testFindByEmailAndRole_Organization() {
        Organization org = new Organization();
        org.setEmail("org@example.com");

        when(organizationRepository.findByEmail("org@example.com"))
                .thenReturn(Optional.of(org));

        var result = authenticationService.findByEmailAndRole("org@example.com", "ORGANIZATION");

        assertTrue(result.isPresent());
        assertEquals(org, result.get());
    }

    @Test
    void testFindByEmailAndRole_InvalidRole() {
        var result = authenticationService.findByEmailAndRole("nope@example.com", "INVALID");
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------
    // TEST: authenticate
    // -------------------------------------------------------

    @Test
    void testAuthenticate_Success() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("hashed123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password123", "hashed123"))
                .thenReturn(true);

        var result = authenticationService.authenticate("test@example.com", "password123", "USER");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void testAuthenticate_Fail_WrongPassword() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("hashed123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong", "hashed123"))
                .thenReturn(false);

        var result = authenticationService.authenticate("test@example.com", "wrong", "USER");

        assertTrue(result.isEmpty());
    }

    @Test
    void testAuthenticate_Fail_NoAccountFound() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        var result = authenticationService.authenticate("missing@example.com", "any", "USER");

        assertTrue(result.isEmpty());
    }
}
