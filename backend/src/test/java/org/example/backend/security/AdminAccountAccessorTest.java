package org.example.backend.security;

import org.example.backend.admin.Admin;
import org.example.backend.admin.AdminRepository;
import org.example.backend.errorHandler.ResourceNotFoundException;
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
class AdminAccountAccessorTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminAccountAccessor accessor;

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = Admin.builder().email("admin@example.com").password("old-encoded").build();
    }

    @Test
    void role_ReturnsAdmin() {
        assertEquals(AccountRole.ADMIN, accessor.role());
    }

    @Test
    void findByEmail_AdminExists_ReturnsAuthenticatable() {
        when(adminRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        Optional<Authenticatable> result = accessor.findByEmail("admin@example.com");

        assertTrue(result.isPresent());
        assertEquals("admin@example.com", result.get().getEmail());
    }

    @Test
    void findByEmail_AdminDoesNotExist_ReturnsEmpty() {
        when(adminRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertTrue(accessor.findByEmail("missing@example.com").isEmpty());
    }

    @Test
    void updatePassword_AdminExists_EncodesAndSaves() {
        when(adminRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        accessor.updatePassword("admin@example.com", "new-encoded");

        assertEquals("new-encoded", admin.getPassword());
        verify(adminRepository).save(admin);
    }

    @Test
    void updatePassword_AdminDoesNotExist_ThrowsResourceNotFound() {
        when(adminRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accessor.updatePassword("missing@example.com", "new-encoded"));
        verify(adminRepository, never()).save(any());
    }
}
