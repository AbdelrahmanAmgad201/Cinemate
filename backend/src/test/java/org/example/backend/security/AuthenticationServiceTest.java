package org.example.backend.security;

import org.example.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthenticationServiceTest {

    @Mock
    private AccountRegistry accountRegistry;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authService;

    private final String email = "test@example.com";
    private final String rawPassword = "password";
    private final String encodedPassword = "encodedPassword";

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);
    }

    // -------------------------------------------------------------------------
    // TEST: findByEmailAndRole → delegates to AccountRegistry
    // -------------------------------------------------------------------------
    @Test
    void testFindByEmailAndRoleDelegatesToRegistry() {
        when(accountRegistry.findByEmailAndRole(email, "USER")).thenReturn(Optional.of(user));

        Optional<Authenticatable> result = authService.findByEmailAndRole(email, "USER");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void testFindByEmailAndRoleUnknown() {
        when(accountRegistry.findByEmailAndRole(email, "UNKNOWN")).thenReturn(Optional.empty());

        Optional<Authenticatable> result = authService.findByEmailAndRole(email, "UNKNOWN");

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // TEST: authenticate → success
    // -------------------------------------------------------------------------
    @Test
    void testAuthenticateSuccess() {
        when(accountRegistry.findByEmailAndRole(email, "USER")).thenReturn(Optional.of(user));
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
        when(accountRegistry.findByEmailAndRole(email, "USER")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        Optional<Authenticatable> result = authService.authenticate(email, rawPassword, "USER");

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // TEST: authenticate → unknown role
    // -------------------------------------------------------------------------
    @Test
    void testAuthenticateUnknownRole() {
        when(accountRegistry.findByEmailAndRole(email, "UNKNOWN")).thenReturn(Optional.empty());

        Optional<Authenticatable> result = authService.authenticate(email, rawPassword, "UNKNOWN");

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // TEST: updatePassword → wrong old password throws
    // -------------------------------------------------------------------------
    @Test
    void testUpdatePasswordWrongOldPasswordThrows() {
        when(accountRegistry.findByEmailAndRole(email, "USER")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-old-password", encodedPassword)).thenReturn(false);

        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setOldPassword("wrong-old-password");
        dto.setNewPassword("new-password");

        assertThrows(WrongPasswordException.class, () -> authService.updatePassword(email, dto, "USER"));
        verify(accountRegistry, never()).updatePassword(anyString(), anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // TEST: updatePassword → success delegates the encoded new password to the registry
    // -------------------------------------------------------------------------
    @Test
    void testUpdatePasswordSuccessDelegatesToRegistry() {
        when(accountRegistry.findByEmailAndRole(email, "USER")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded-password");

        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setOldPassword(rawPassword);
        dto.setNewPassword("new-password");

        authService.updatePassword(email, dto, "USER");

        verify(accountRegistry).updatePassword(email, "USER", "new-encoded-password");
    }
}
