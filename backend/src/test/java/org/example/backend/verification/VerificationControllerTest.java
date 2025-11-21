package org.example.backend.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VerificationController.class)
@AutoConfigureMockMvc(addFilters = false)   // DISABLE SECURITY FOR TESTING
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

        Mockito.when(verificationService.verifyEmail(Mockito.any(VerificationDTO.class)))
                .thenReturn(true);

        mockMvc.perform(post("/api/verification/v1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Verification successful")))
                .andExpect(jsonPath("$.data", is(true)));
    }

    @Test
    void testVerifyFailed() throws Exception {

        VerificationDTO dto = new VerificationDTO();
        dto.setEmail("wrong@example.com");
        dto.setCode(5555);

        Mockito.when(verificationService.verifyEmail(Mockito.any(VerificationDTO.class)))
                .thenReturn(false);

        mockMvc.perform(post("/api/verification/v1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid or expired code")))
                .andExpect(jsonPath("$.data", is(false)));
    }

}
