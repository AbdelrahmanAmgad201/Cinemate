package org.example.backend.security;

import org.example.backend.errorHandler.ResourceNotFoundException;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountAccessorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAccountAccessor accessor;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("old-encoded");
    }

    @Test
    void role_ReturnsUser() {
        assertEquals(AccountRole.USER, accessor.role());
    }

    @Test
    void findByEmail_UserExists_ReturnsAuthenticatable() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        Optional<Authenticatable> result = accessor.findByEmail("jane@example.com");

        assertTrue(result.isPresent());
        assertEquals("jane@example.com", result.get().getEmail());
    }

    @Test
    void findByEmail_UserDoesNotExist_ReturnsEmpty() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertTrue(accessor.findByEmail("missing@example.com").isEmpty());
    }

    @Test
    void updatePassword_UserExists_EncodesAndSaves() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        accessor.updatePassword("jane@example.com", "new-encoded");

        assertEquals("new-encoded", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void updatePassword_UserDoesNotExist_ThrowsResourceNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accessor.updatePassword("missing@example.com", "new-encoded"));
        verify(userRepository, never()).save(any());
    }
}
