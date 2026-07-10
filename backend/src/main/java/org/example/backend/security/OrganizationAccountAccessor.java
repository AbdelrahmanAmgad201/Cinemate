package org.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.example.backend.errorHandler.ResourceNotFoundException;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class OrganizationAccountAccessor implements AccountAccessor {

    private final OrganizationRepository organizationRepository;

    @Override
    public AccountRole role() {
        return AccountRole.ORGANIZATION;
    }

    @Override
    public Optional<Authenticatable> findByEmail(String email) {
        return organizationRepository.findByEmail(email).map(org -> (Authenticatable) org);
    }

    @Override
    public void updatePassword(String email, String encodedPassword) {
        Organization organization = organizationRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        organization.setPassword(encodedPassword);
        organizationRepository.save(organization);
    }
}
