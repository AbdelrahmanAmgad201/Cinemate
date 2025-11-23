package org.example.backend.organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrganizationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private OrganizationController organizationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(organizationController).build();
    }

    @Test
    void testGetProfile() throws Exception {
        mockMvc.perform(get("/api/organization/v1/profile")
                        .requestAttr("userId", 1L)
                        .requestAttr("userEmail", "org@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("User profile for ID: 1, Email: org@example.com"));
    }

    @Test
    void testSetPersonalData() throws Exception {
        Long userId = 1L;
        OrganizationDataDTO dto = new OrganizationDataDTO();
        dto.setName("New Name");
        dto.setAbout("New About");

        when(organizationService.setOrganizationData(eq(userId), any(OrganizationDataDTO.class)))
                .thenReturn("User data updated successfully");

        String jsonBody = """
                {
                    "name": "New Name",
                    "about": "New About"
                }
                """;

        mockMvc.perform(post("/api/organization/v1/set-organization-data")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(content().string("User data updated successfully"));

        verify(organizationService, times(1))
                .setOrganizationData(eq(userId), any(OrganizationDataDTO.class));
    }
}
