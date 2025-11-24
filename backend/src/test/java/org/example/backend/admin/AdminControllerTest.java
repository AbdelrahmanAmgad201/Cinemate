package org.example.backend.admin;

import org.example.backend.movie.Movie;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MovieService movieService;

    @Mock
    private AdminService adminService;

    @Mock
    private RequestsService requestsService;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    // ============================================================
    // ✅ TEST: /v1/find_admin_requests
    // ============================================================
    @Test
    void testFindAllAdminRequests() throws Exception {
        List<Movie> movies = Arrays.asList(new Movie(), new Movie());

        when(movieService.findAllAdminRequests()).thenReturn(movies);

        mockMvc.perform(post("/api/admin/v1/find_admin_requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(movieService, times(1)).findAllAdminRequests();
    }

    // ============================================================
    // ✅ TEST: /v1/decline_request
    // ============================================================
    @Test
    void testDeclineRequest() throws Exception {
        mockMvc.perform(post("/api/admin/v1/decline_request")
                        .param("requestId", "5"))
                .andExpect(status().isOk());

        verify(adminService, times(1)).declineRequest(5L);
    }

    // ============================================================
    // ✅ TEST: /v1/accept_request
    // ============================================================
    @Test
    void testAcceptRequest() throws Exception {

        String jsonBody = """
                {
                    "requestId": 10,
                    "movieId": 20
                }
                """;

        mockMvc.perform(post("/api/admin/v1/accept_request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk());

        verify(adminService, times(1)).acceptRequests(any(AcceptDTO.class));
    }

    // ============================================================
    // ✅ TEST: /v1/get_pending_requests
    // ============================================================
    @Test
    void testGetPendingRequests() throws Exception {
        List<Requests> mockRequests = Arrays.asList(new Requests(), new Requests());

        when(requestsService.getAllPendingRequests()).thenReturn(mockRequests);

        mockMvc.perform(post("/api/admin/v1/get_pending_requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(requestsService, times(1)).getAllPendingRequests();
    }
}
