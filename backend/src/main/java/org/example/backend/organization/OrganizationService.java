package org.example.backend.organization;

import jakarta.transaction.Transactional;
import org.example.backend.user.Gender;
import org.example.backend.user.User;
import org.example.backend.user.UserDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;

    public Organization addOrganization(String email, String password) {
        Organization organization = Organization.builder()
                .email(email)
                .password(password)
                .name("Test Organization")
                .build();
        return organizationRepository.save(organization);
    }


    @Transactional
    public String setOrganizationData(Long userId, OrganizationDataDTO organizationDataDTO) {
        Organization organization = organizationRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        organization.setName(organizationDataDTO.getName());
        organization.setAbout(organizationDataDTO.getAbout());

        organizationRepository.save(organization);

        return "User data updated successfully";
    }
}
