package org.example.backend.organization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;

    public void addOrganization(String email, String password) {
        Organization organization = Organization.builder()
                .email(email)
                .password(password)
                .name("Test Organization")
                .build();
        organizationRepository.save(organization);
    }
}
