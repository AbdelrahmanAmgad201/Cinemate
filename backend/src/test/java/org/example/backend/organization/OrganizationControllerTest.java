package org.example.backend.organization;

import org.example.backend.movie.MovieAddDTO;
import org.example.backend.movie.MovieService;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrganizationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private MovieService movieService;

    @Mock
    private RequestsService requestsService;

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

    // =====================================================
    // ✅ TEST: /v1/add-movie
    // =====================================================
    @Test
    void testAddMovie() throws Exception {

        when(organizationService.requestMovie(any(MovieAddDTO.class))).thenReturn(99L);

        String jsonBody = """
            {
                "name": "Movie name",
                "description": "Description",
                "movieUrl": "http://movie.com",
                "thumbnailUrl": "http://thumb.com",
                "trailerUrl": "http://trailer.com",
                "duration": 120,
                "genre": "ACTION",
                "organizationId": 10
            }
            """;

        mockMvc.perform(post("/api/organization/v1/add-movie")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(content().string("99"));

        verify(organizationService, times(1)).requestMovie(any(MovieAddDTO.class));
        verifyNoInteractions(movieService); // MUST NOT call movieService
    }

    // =====================================================
    // ✅ NEW TEST: /v1/get_org_rquests
    // =====================================================
    @Test
    void testGetOrgRequests() throws Exception {

        List<Requests> mockList = Arrays.asList(
                new Requests(), new Requests()
        );

        when(requestsService.getAllOrganizationRequests(10L)).thenReturn(mockList);

        mockMvc.perform(post("/api/organization/v1/get_org_rquests?orgId=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(requestsService, times(1)).getAllOrganizationRequests(10L);
    }
}
