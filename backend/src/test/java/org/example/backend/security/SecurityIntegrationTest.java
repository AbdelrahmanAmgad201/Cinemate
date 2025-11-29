package org.example.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "USER")
    void userEndpointAccessibleForUser() throws Exception {
        mockMvc.perform(get("/api/user/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("USER OK"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpointForbiddenForUser() throws Exception {
        mockMvc.perform(get("/api/user/test"))
                .andExpect(status().isForbidden()); // USER-only endpoint blocked for ADMIN role
    }
}
