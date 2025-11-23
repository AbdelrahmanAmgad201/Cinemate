package org.example.backend.organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------- addOrganization ----------------
    @Test
    void testAddOrganization() {
        String email = "org@example.com";
        String password = "password";

        Organization orgToSave = Organization.builder()
                .email(email)
                .password(password)
                .name("Test Organization")
                .build();

        Organization savedOrg = Organization.builder()
                .id(1L)
                .email(email)
                .password(password)
                .name("Test Organization")
                .build();

        when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrg);

        Organization result = organizationService.addOrganization(email, password);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(email, result.getEmail());
        assertEquals("Test Organization", result.getName());

        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    // ---------------- setOrganizationData ----------------
    @Test
    void testSetOrganizationDataSuccess() {
        Long orgId = 1L;
        Organization existingOrg = Organization.builder()
                .id(orgId)
                .name("Old Name")
                .about("Old About")
                .build();

        OrganizationDataDTO updateDTO = new OrganizationDataDTO();
        updateDTO.setName("New Name");
        updateDTO.setAbout("New About");

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(existingOrg));
        when(organizationRepository.save(existingOrg)).thenReturn(existingOrg);

        String result = organizationService.setOrganizationData(orgId, updateDTO);

        assertEquals("User data updated successfully", result);
        assertEquals("New Name", existingOrg.getName());
        assertEquals("New About", existingOrg.getAbout());

        verify(organizationRepository, times(1)).findById(orgId);
        verify(organizationRepository, times(1)).save(existingOrg);
    }

    @Test
    void testSetOrganizationDataNotFound() {
        Long orgId = 1L;
        OrganizationDataDTO updateDTO = new OrganizationDataDTO();
        updateDTO.setName("New Name");
        updateDTO.setAbout("New About");

        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                organizationService.setOrganizationData(orgId, updateDTO));

        assertEquals("User not found", exception.getMessage());
        verify(organizationRepository, times(1)).findById(orgId);
        verify(organizationRepository, never()).save(any());
    }
}
