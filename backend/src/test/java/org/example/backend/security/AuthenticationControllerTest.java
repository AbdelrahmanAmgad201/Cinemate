package org.example.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.user.User;
import org.example.backend.user.UserAlreadyExistsException;
import org.example.backend.user.UserService;
import org.example.backend.verification.Verification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Routed through real MockMvc (instead of bare @InjectMocks) so @Valid validation
 * errors and GlobalExceptionHandler mapping are actually exercised, not just the
 * controller method body. Security filters are disabled (addFilters = false) since
 * authorization is covered separately by SecurityIntegrationTest and
 * GatewayAuthenticationFilterTest — this class only tests MVC dispatch.
 */
@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JWTProvider jwtTokenProvider;

    @MockBean
    private UserService userService;

    @MockBean
    private OAuthExchangeService oAuthExchangeService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private RefreshTokenCookie refreshTokenCookie;

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private static UpdatePasswordDTO updatePasswordDTO(String oldPassword, String newPassword) {
        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setOldPassword(oldPassword);
        dto.setNewPassword(newPassword);
        return dto;
    }

    @Test
    void login_ValidCredentials_ReturnsTokenAndSetsCookie() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");

        when(authenticationService.authenticate("test@example.com", "password", "USER"))
                .thenReturn(Optional.of(mockUser));
        when(jwtTokenProvider.generateAccessToken(mockUser)).thenReturn("mockToken");
        when(refreshTokenService.issue(anyString(), anyString())).thenReturn("mockRefresh");
        when(refreshTokenCookie.build(anyString()))
                .thenReturn(ResponseCookie.from("refresh_token", "mockRefresh").build());

        mockMvc.perform(post("/api/auth/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CredentialsRequest("test@example.com", "password", "USER"))))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.accessToken").value("mockToken"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        when(authenticationService.authenticate("wrong@example.com", "wrong", "USER"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CredentialsRequest("wrong@example.com", "wrong", "USER"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_MissingEmail_ReturnsBadRequestFromValidation() throws Exception {
        mockMvc.perform(post("/api/auth/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CredentialsRequest("", "password", "USER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email")));
    }

    @Test
    void login_MalformedEmail_ReturnsBadRequestFromValidation() throws Exception {
        mockMvc.perform(post("/api/auth/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CredentialsRequest("not-an-email", "password", "USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_NewUser_ReturnsCreated() throws Exception {
        Verification mockVerification = new Verification();
        when(userService.signUp(org.mockito.ArgumentMatchers.any())).thenReturn(mockVerification);

        mockMvc.perform(post("/api/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CredentialsRequest("new@example.com", "password", "USER"))))
                .andExpect(status().isCreated());
    }

    @Test
    void signUp_UserAlreadyExists_ReturnsConflict() throws Exception {
        when(userService.signUp(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new UserAlreadyExistsException("User already exists"));

        mockMvc.perform(post("/api/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CredentialsRequest("existing@example.com", "password", "USER"))))
                .andExpect(status().isConflict())
                .andExpect(content().string("User already exists"));
    }

    @Test
    void signUp_BlankPassword_ReturnsBadRequestFromValidation() throws Exception {
        mockMvc.perform(post("/api/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CredentialsRequest("new@example.com", "", "USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePassword_Valid_ReturnsSuccessMessage() throws Exception {
        mockMvc.perform(put("/api/auth/v1/password")
                        .requestAttr("userEmail", "test@example.com")
                        .requestAttr("userRole", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updatePasswordDTO("oldpass123", "newpass123"))))
                .andExpect(status().isOk())
                .andExpect(content().string("password updated successfully"));
    }

    @Test
    void updatePassword_NewPasswordTooShort_ReturnsBadRequestFromValidation() throws Exception {
        mockMvc.perform(put("/api/auth/v1/password")
                        .requestAttr("userEmail", "test@example.com")
                        .requestAttr("userRole", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updatePasswordDTO("oldpass123", "short"))))
                .andExpect(status().isBadRequest());
    }
}
