package org.example.backend.security;

import org.example.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountRegistryTest {

    @Mock
    private AccountAccessor userAccessor;

    @Mock
    private AccountAccessor adminAccessor;

    private AccountRegistry registry;

    @BeforeEach
    void setup() {
        lenient().when(userAccessor.role()).thenReturn(AccountRole.USER);
        lenient().when(adminAccessor.role()).thenReturn(AccountRole.ADMIN);
        registry = new AccountRegistry(List.of(userAccessor, adminAccessor));
        // Constructing the registry itself calls role() on each accessor; reset so the
        // per-test verifyNoInteractions() checks only see interactions from the test body.
        clearInvocations(userAccessor, adminAccessor);
    }

    @Test
    void findByEmailAndRole_dispatchesToMatchingAccessor() {
        User user = new User();
        user.setEmail("test@example.com");
        when(userAccessor.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Optional<Authenticatable> result = registry.findByEmailAndRole("test@example.com", "USER");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verifyNoInteractions(adminAccessor);
    }

    @Test
    void findByEmailAndRole_normalizesRolePrefix() {
        when(adminAccessor.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        registry.findByEmailAndRole("admin@example.com", "ROLE_ADMIN");

        verify(adminAccessor).findByEmail("admin@example.com");
    }

    @Test
    void findByEmailAndRole_unknownRoleReturnsEmptyWithoutTouchingAccessors() {
        Optional<Authenticatable> result = registry.findByEmailAndRole("test@example.com", "ORGANIZATION");

        assertTrue(result.isEmpty());
        verifyNoInteractions(userAccessor, adminAccessor);
    }

    @Test
    void updatePassword_dispatchesToMatchingAccessor() {
        registry.updatePassword("test@example.com", "USER", "encoded");

        verify(userAccessor).updatePassword("test@example.com", "encoded");
        verifyNoInteractions(adminAccessor);
    }

    @Test
    void updatePassword_unknownRoleThrows() {
        assertThrows(RuntimeException.class,
                () -> registry.updatePassword("test@example.com", "ORGANIZATION", "encoded"));
        verifyNoInteractions(userAccessor, adminAccessor);
    }
}
