package org.example.backend.user;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.security.CredentialsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ActiveProfiles("test")
class UserControllerTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setup() {
        userService = mock(UserService.class);

        UserController userController = new UserController();
        // manually inject service
        var field = UserController.class.getDeclaredFields()[0];
        field.setAccessible(true);
        try {
            field.set(userController, userService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .build();
    }

    @Test
    void testGetProfile() throws Exception {
        mockMvc.perform(get("/api/user/v1/profile")
                        .requestAttr("userId", 5L)
                        .requestAttr("userEmail", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("User profile for ID: 5, Email: test@example.com"));
    }

    @Test
    void testSetPersonalData() throws Exception {
        UserDataDTO dto = new UserDataDTO();
        dto.setFirstName("John");
        dto.setLastName("Doe");

        when(userService.setUserData(eq(5L), any(UserDataDTO.class)))
                .thenReturn("User data updated successfully");

        mockMvc.perform(post("/api/user/v1/set-user-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "John",
                                    "lastName": "Doe"
                                }
                                """)
                        .requestAttr("userId", 5L))
                .andExpect(status().isOk())
                .andExpect(content().string("User data updated successfully"));

        verify(userService).setUserData(eq(5L), any(UserDataDTO.class));
    }
}
