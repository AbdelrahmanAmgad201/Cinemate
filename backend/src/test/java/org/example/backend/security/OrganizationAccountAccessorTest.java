package org.example.backend.security;

import org.example.backend.errorHandler.ResourceNotFoundException;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
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
class OrganizationAccountAccessorTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationAccountAccessor accessor;

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = Organization.builder().email("org@example.com").password("old-encoded").build();
    }

    @Test
    void role_ReturnsOrganization() {
        assertEquals(AccountRole.ORGANIZATION, accessor.role());
    }

    @Test
    void findByEmail_OrganizationExists_ReturnsAuthenticatable() {
        when(organizationRepository.findByEmail("org@example.com")).thenReturn(Optional.of(organization));

        Optional<Authenticatable> result = accessor.findByEmail("org@example.com");

        assertTrue(result.isPresent());
        assertEquals("org@example.com", result.get().getEmail());
    }

    @Test
    void findByEmail_OrganizationDoesNotExist_ReturnsEmpty() {
        when(organizationRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertTrue(accessor.findByEmail("missing@example.com").isEmpty());
    }

    @Test
    void updatePassword_OrganizationExists_EncodesAndSaves() {
        when(organizationRepository.findByEmail("org@example.com")).thenReturn(Optional.of(organization));

        accessor.updatePassword("org@example.com", "new-encoded");

        assertEquals("new-encoded", organization.getPassword());
        verify(organizationRepository).save(organization);
    }

    @Test
    void updatePassword_OrganizationDoesNotExist_ThrowsResourceNotFound() {
        when(organizationRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accessor.updatePassword("missing@example.com", "new-encoded"));
        verify(organizationRepository, never()).save(any());
    }
}
