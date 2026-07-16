package org.example.backend.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.security.RefreshTokenCookie;
import org.example.backend.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(Jackson2AutoConfiguration.class)
@WebMvcTest(controllers = VerificationController.class)
@AutoConfigureMockMvc(addFilters = false)   // DISABLE SECURITY FOR TESTING
@ActiveProfiles("test")
class VerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationService verificationService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private RefreshTokenCookie refreshTokenCookie;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // A successful verify issues a refresh token then builds a cookie around it.
        // Both must be stubbed: anyString() doesn't match a null argument, so leaving
        // issue() unstubbed makes it return null, which then fails to match
        // build(anyString()) too and falls through to a null cookie.
        Mockito.when(refreshTokenService.issue(anyString(), anyString()))
                .thenReturn("mock-refresh");
        Mockito.when(refreshTokenCookie.build(anyString()))
                .thenReturn(ResponseCookie.from("refresh_token", "mock-refresh").build());
    }

    @Test
    void testVerifySuccessful() throws Exception {
        VerificationDTO dto = new VerificationDTO();
        dto.setEmail("test@example.com");
        dto.setCode(123456); // must be a real 6-digit code now that VerificationDTO validates the range

        VerificationResponseDTO response = VerificationResponseDTO.builder()
                .success(true)
                .message("Verification successful")
                .accessToken("mock-jwt-token-xyz")
                .id(1L)
                .email("test@example.com")
                .role("USER")
                .build();

        Mockito.when(verificationService.verifyEmail(Mockito.any(VerificationDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/verification/v1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Verification successful")))
                .andExpect(jsonPath("$.accessToken", is("mock-jwt-token-xyz")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    void testVerifyFailed() throws Exception {
        VerificationDTO dto = new VerificationDTO();
        dto.setEmail("wrong@example.com");
        dto.setCode(555555);

        VerificationResponseDTO response = VerificationResponseDTO.builder()
                .success(false)
                .message("Invalid or expired code")
                .build();

        Mockito.when(verificationService.verifyEmail(Mockito.any(VerificationDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/verification/v1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid or expired code")))
                .andExpect(jsonPath("$.accessToken", nullValue()))
                .andExpect(jsonPath("$.id", nullValue()))
                .andExpect(jsonPath("$.email", nullValue()))
                .andExpect(jsonPath("$.role", nullValue()));
    }

    @Test
    void testVerifyOrganizationSuccessful() throws Exception {
        VerificationDTO dto = new VerificationDTO();
        dto.setEmail("org@example.com");
        dto.setCode(999999);

        VerificationResponseDTO response = VerificationResponseDTO.builder()
                .success(true)
                .message("Verification successful")
                .accessToken("mock-org-jwt-token")
                .id(2L)
                .email("org@example.com")
                .role("ORGANIZATION")
                .build();

        Mockito.when(verificationService.verifyEmail(Mockito.any(VerificationDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/verification/v1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Verification successful")))
                .andExpect(jsonPath("$.accessToken", is("mock-org-jwt-token")))
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.email", is("org@example.com")))
                .andExpect(jsonPath("$.role", is("ORGANIZATION")));
    }
}
