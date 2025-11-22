package org.example.backend.security;

import org.example.backend.user.User;
import org.example.backend.user.UserAlreadyExistsException;
import org.example.backend.user.UserService;
import org.example.backend.verification.Verfication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private JWTProvider jwtTokenProvider;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationController authenticationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ------------------ LOGIN TESTS ------------------

    @Test
    void testLoginSuccess() {
        CredentialsRequest request = new CredentialsRequest("test@example.com", "password", "USER");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");

        when(authenticationService.authenticate(
                request.getEmail(),
                request.getPassword(),
                request.getRole()))
                .thenReturn(Optional.of(mockUser));

        when(jwtTokenProvider.generateToken(mockUser)).thenReturn("mockToken");

        ResponseEntity<?> response = authenticationController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("mockToken", body.get("token"));
        assertEquals(1L, body.get("id"));
        assertEquals("test@example.com", body.get("email"));
        assertEquals("ROLE_USER", body.get("role"));
    }

    @Test
    void testLoginFailure() {
        CredentialsRequest request = new CredentialsRequest("wrong@example.com", "wrong", "USER");

        when(authenticationService.authenticate(
                request.getEmail(),
                request.getPassword(),
                request.getRole()))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = authenticationController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Invalid credentials", body.get("error"));
    }

    // ------------------ SIGN-UP TESTS ------------------

    @Test
    void testSignUpSuccess() throws UserAlreadyExistsException {
        CredentialsRequest request = new CredentialsRequest("new@example.com", "password", "USER");

        Verfication mockVerification = new Verfication();

        when(userService.signUp(request)).thenReturn(mockVerification);

        ResponseEntity<?> response = authenticationController.signUp(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockVerification, response.getBody());
    }

    @Test
    void testSignUpUserAlreadyExists() throws UserAlreadyExistsException {
        CredentialsRequest request = new CredentialsRequest("existing@example.com", "password", "USER");

        when(userService.signUp(request))
                .thenThrow(new UserAlreadyExistsException("User already exists"));

        ResponseEntity<?> response = authenticationController.signUp(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("User already exists", response.getBody());
    }
}
