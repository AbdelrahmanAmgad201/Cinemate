package org.example.backend.security;

import org.example.backend.user.User;
import org.example.backend.user.UserAlreadyExistsException;
import org.example.backend.user.UserService;
import org.example.backend.verification.Verification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private JWTProvider jwtTokenProvider;

    @Mock
    private UserService userService;

    @Mock
    private OAuthExchangeService oAuthExchangeService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenCookie refreshTokenCookie;

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

        when(jwtTokenProvider.generateAccessToken(mockUser)).thenReturn("mockToken");
        when(refreshTokenService.issue(anyString(), anyString())).thenReturn("mockRefresh");
        when(refreshTokenCookie.build(anyString()))
                .thenReturn(ResponseCookie.from("refresh_token", "mockRefresh").build());

        ResponseEntity<?> response = authenticationController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("mockToken", body.get("accessToken"));
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

        Verification mockVerification = new Verification();

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
