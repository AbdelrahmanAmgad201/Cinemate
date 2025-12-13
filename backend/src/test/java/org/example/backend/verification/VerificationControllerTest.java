package org.example.backend.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VerificationController.class)
@AutoConfigureMockMvc(addFilters = false)   // DISABLE SECURITY FOR TESTING
@ActiveProfiles("test")
class VerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationService verificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testVerifySuccessful() throws Exception {
        VerificationDTO dto = new VerificationDTO();
        dto.setEmail("test@example.com");
        dto.setCode(1234);

        VerificationResponseDTO response = VerificationResponseDTO.builder()
                .success(true)
                .message("Verification successful")
                .token("mock-jwt-token-xyz")
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
                .andExpect(jsonPath("$.token", is("mock-jwt-token-xyz")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    void testVerifyFailed() throws Exception {
        VerificationDTO dto = new VerificationDTO();
        dto.setEmail("wrong@example.com");
        dto.setCode(5555);

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
                .andExpect(jsonPath("$.token", nullValue()))
                .andExpect(jsonPath("$.id", nullValue()))
                .andExpect(jsonPath("$.email", nullValue()))
                .andExpect(jsonPath("$.role", nullValue()));
    }

    @Test
    void testVerifyOrganizationSuccessful() throws Exception {
        VerificationDTO dto = new VerificationDTO();
        dto.setEmail("org@example.com");
        dto.setCode(9999);

        VerificationResponseDTO response = VerificationResponseDTO.builder()
                .success(true)
                .message("Verification successful")
                .token("mock-org-jwt-token")
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
                .andExpect(jsonPath("$.token", is("mock-org-jwt-token")))
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.email", is("org@example.com")))
                .andExpect(jsonPath("$.role", is("ORGANIZATION")));
    }
}