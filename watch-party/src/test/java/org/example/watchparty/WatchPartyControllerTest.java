package org.example.watchparty;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.watchparty.dtos.WatchPartyCreatedResponse;
import org.example.watchparty.dtos.WatchPartyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WatchPartyController.class)
@AutoConfigureMockMvc(addFilters = false)
class WatchPartyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WatchPartyService watchPartyService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void create_WithUserId_Returns201() throws Exception {
        WatchPartyCreatedResponse response = WatchPartyCreatedResponse.builder()
                .partyId("party-123")
                .movieId(100L)
                .status("ACTIVE")
                .build();
        when(watchPartyService.create(1L, "Test User", 100L)).thenReturn(response);

        mockMvc.perform(post("/api/watch-party/v1/100")
                        .requestAttr("userId", 1L)
                        .requestAttr("userName", "Test User"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partyId").value("party-123"))
                .andExpect(jsonPath("$.movieId").value(100))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_WithoutUserId_Returns401() throws Exception {
        mockMvc.perform(post("/api/watch-party/v1/100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_ExistingParty_Returns200() throws Exception {
        WatchPartyResponse response = new WatchPartyResponse();
        response.setPartyId("party-123");
        response.setStatus("ACTIVE");

        when(watchPartyService.get("party-123")).thenReturn(response);

        mockMvc.perform(get("/api/watch-party/v1/party-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partyId").value("party-123"));
    }

    @Test
    void join_WithUserId_Returns200() throws Exception {
        WatchPartyResponse response = new WatchPartyResponse();
        response.setPartyId("party-123");

        when(watchPartyService.join(2L, "User 2", "party-123")).thenReturn(response);

        mockMvc.perform(put("/api/watch-party/v1/party-123/members")
                        .requestAttr("userId", 2L)
                        .requestAttr("userName", "User 2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partyId").value("party-123"));
    }

    @Test
    void leave_WithUserId_Returns200() throws Exception {
        mockMvc.perform(delete("/api/watch-party/v1/party-123/members")
                        .requestAttr("userId", 2L))
                .andExpect(status().isOk());

        verify(watchPartyService).leave(2L, "party-123");
    }

    @Test
    void delete_WithUserId_Returns204() throws Exception {
        mockMvc.perform(delete("/api/watch-party/v1/party-123")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNoContent());

        verify(watchPartyService).delete(1L, "party-123");
    }
}
