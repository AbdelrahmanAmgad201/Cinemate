package org.example.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.common.dto.UpdateNameRequest;
import org.example.backend.movie.MovieDetailsDTO;
import org.example.backend.movie.MovieService;
import org.example.backend.movie.OneMovieOverView;
import org.example.backend.requests.RequestsResponse;
import org.example.backend.requests.RequestsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Routed through real MockMvc (instead of bare @InjectMocks) so @Valid validation
 * errors and GlobalExceptionHandler mapping are actually exercised. Security filters
 * are disabled (addFilters = false) since authorization is covered separately by
 * SecurityIntegrationTest and GatewayAuthenticationFilterTest — the "userId" request
 * attribute that GatewayAuthenticationFilter would normally set is supplied directly
 * on each request instead.
 */
@Import(Jackson2AutoConfiguration.class)
@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private RequestsService requestsService;

    @MockitoBean
    private MovieService movieService;

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    void findAllAdminRequests_ReturnsRequestsForCurrentAdmin() throws Exception {
        RequestsResponse req = RequestsResponse.builder().id(1L).build();
        when(requestsService.getAllAdminRequests(1L)).thenReturn(List.of(req));

        mockMvc.perform(get("/api/admin/v1/my-requests").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findAllAdminRequests_NoRequests_ReturnsEmptyArray() throws Exception {
        when(requestsService.getAllAdminRequests(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/v1/my-requests").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void declineRequest_Success_ReturnsConfirmationMessage() throws Exception {
        mockMvc.perform(post("/api/admin/v1/requests/5/decline")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("declined successfully")));
    }

    @Test
    void declineRequest_ServiceThrows_MapsToInternalServerError() throws Exception {
        doThrow(new RuntimeException("Movie not found"))
                .when(adminService).declineRequest(1L, 5L);

        mockMvc.perform(post("/api/admin/v1/requests/5/decline")
                        .requestAttr("userId", 1L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void acceptRequest_Success_ReturnsConfirmationMessage() throws Exception {
        mockMvc.perform(post("/api/admin/v1/requests/10/accept")
                        .requestAttr("userId", 2L))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("accepted successfully")));
    }

    @Test
    void getRequestedMovie_ReturnsMovieDetails() throws Exception {
        MovieDetailsDTO dto = MovieDetailsDTO.builder().movieID(3L).name("Test Movie").build();
        when(adminService.getRequestedMovie(3L)).thenReturn(dto);

        mockMvc.perform(get("/api/admin/v1/requests/3/movie"))
                .andExpect(status().isOk());
    }

    @Test
    void getSpecificMovieOverview_ReturnsOverview() throws Exception {
        OneMovieOverView overview = new OneMovieOverView(10L, 5L, 2L, 1L, 4.5, 3L);
        when(movieService.getMovieStatsByMovieId(7L)).thenReturn(overview);

        mockMvc.perform(get("/api/admin/v1/movies/7/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5));
    }

    @Test
    void getAdminProfile_ReturnsProfileForCurrentAdmin() throws Exception {
        when(adminService.getAdminProfile(1L))
                .thenReturn(new AdminProfileDTO("John Doe", "john@example.com", "ADMIN"));

        mockMvc.perform(get("/api/admin/v1/profile").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void addAdmin_Valid_ReturnsOk() throws Exception {
        AddAdminDTO dto = new AddAdminDTO();
        dto.setName("New Admin");
        dto.setEmail("admin@example.com");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/admin/v1/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void addAdmin_BlankName_ReturnsBadRequestFromValidation() throws Exception {
        AddAdminDTO dto = new AddAdminDTO();
        dto.setName("");
        dto.setEmail("admin@example.com");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/admin/v1/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addAdmin_PasswordTooShort_ReturnsBadRequestFromValidation() throws Exception {
        AddAdminDTO dto = new AddAdminDTO();
        dto.setName("New Admin");
        dto.setEmail("admin@example.com");
        dto.setPassword("short");

        mockMvc.perform(post("/api/admin/v1/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAdminName_Valid_ReturnsSuccessMessage() throws Exception {
        UpdateNameRequest nameRequest = new UpdateNameRequest();
        nameRequest.setName("Updated Name");

        mockMvc.perform(put("/api/admin/v1/name")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(nameRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Name updated successfully"));
    }

    @Test
    void updateAdminName_BlankName_ReturnsBadRequestFromValidation() throws Exception {
        UpdateNameRequest nameRequest = new UpdateNameRequest();
        nameRequest.setName("");

        mockMvc.perform(put("/api/admin/v1/name")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(nameRequest)))
                .andExpect(status().isBadRequest());
    }
}
