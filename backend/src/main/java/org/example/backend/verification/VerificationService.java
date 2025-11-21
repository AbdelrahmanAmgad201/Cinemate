package org.example.backend.verification;

import jakarta.transaction.Transactional;
import org.example.backend.admin.Admin;
import org.example.backend.admin.AdminRepository;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
import org.example.backend.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Service
public class VerificationService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VerificationRepository verificationRepository;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    private void addUser(String email, String password) {
        User user = User.builder()
                .email(email)
                .password(password)
                .build();
        userRepository.save(user);
    }
    private void addOrganization(String email, String password) {
        Organization organization = Organization.builder()
                .email(email)
                .password(password)
                .name("Test Organization")
                .build();
        organizationRepository.save(organization);
    }

    private Optional<Verfication> verify(String Email, int code) {
        Optional<Verfication> verification = verificationRepository.findByEmail(Email);
        try {
            if (verification.isPresent()) {
                int verifiedCode =verification.get().getCode();
                if (verifiedCode == code) {
                    return verification;
                }
                else  {
                    return Optional.empty();
                }
            }
            else {
                throw new RuntimeException("email not found in DB");
            }
        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Transactional
    public boolean sendVerificationEmail(String toEmail, int code) {
        try {
            String url = "https://api.sendgrid.com/v3/mail/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(sendGridApiKey);

            Map<String, Object> body = new HashMap<>();

            body.put("from", Map.of("email", fromEmail));

            body.put("personalizations", List.of(
                    Map.of(
                            "to", List.of(Map.of("email", toEmail)),
                            "subject", "Your Verification Code"
                    )
            ));

            body.put("content", List.of(
                    Map.of(
                            "type", "text/plain",
                            "value", "Your verification code is: " + code
                    )
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, request, String.class);

            return true;

        } catch (Exception e) {
            e.printStackTrace(); // <--- IMPORTANT
            return false;
        }
    }

    @Transactional
    public boolean verifyEmail(VerificationDTO verificationDTO) {
        try {
            String email = verificationDTO.getEmail();
            int code = verificationDTO.getCode();
            Optional<Verfication> verification = verify(email, code);
            if (verification.isPresent()) {
                String password = verification.get().getPassword();
                switch (verification.get().getRole()) {
                    case "ORGANIZATION":
                        addOrganization(email, password);
                        break;
                    case "USER":
                        addUser(email, password);
                        break;
                }
                verificationRepository.delete(verification.get());
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Transactional
    public void deleteOldVerifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        verificationRepository.deleteOlderThan(cutoff);
    }
}
