package org.example.backend.verification;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.admin.Admin;
import org.example.backend.admin.AdminRepository;
import org.example.backend.admin.AdminService;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
import org.example.backend.security.Authenticatable;
import org.example.backend.security.JWTProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.backend.organization.OrganizationService;
import org.example.backend.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@Service
public class VerificationService {

    @Autowired
    private VerificationRepository verificationRepository;

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private JWTProvider jwtTokenProvider;

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;


    private final RestTemplate restTemplate = new RestTemplate();
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AdminRepository adminRepository;
    private final Random random = new Random();

    @Transactional
    public Verfication forgetPassword(String email,String role) {
        int code = 100000 + random.nextInt(900000);
        if(sendVerificationEmail(email, code)){
            return addVerfication(email ,code,role);
        }
        else{
            return new Verfication();
        }
    }

    @Transactional
    public Verfication addVerfication(String email, String password, int code, String role) {
        String hashedPassword = passwordEncoder.encode(password);
        Optional<Verfication> oldVerification = verificationRepository.findByEmail(email);
        oldVerification.ifPresent(verificationRepository::delete);
        Verfication verfication = Verfication.builder()
                .email(email)
                .password(hashedPassword)
                .code(code)
                .role(role)
                .build();
        return verificationRepository.save(verfication);
    }

    private Verfication addVerfication(String email, int code,String role) {
        Optional<Verfication> oldVerification = verificationRepository.findByEmail(email);
        oldVerification.ifPresent(verificationRepository::delete);
        Verfication verfication = Verfication.builder()
                .email(email)
                .code(code)
                .role(role)
                .build();
        return verificationRepository.save(verfication);
    }

    @Transactional
    public void updatePasswordByVerificationCode(String email, UpdatePasswordWithVerificationDTO updatePasswordWithVerificationDTO,String role) {
        int verificationCode = updatePasswordWithVerificationDTO.getCode();
        String password = updatePasswordWithVerificationDTO.getPassword();
        Optional<Verfication> verification =verify(email,verificationCode);
        if(verification.isPresent()) {
            switch (role) {
                case "ROLE_USER" -> updateUserPassword(email,password);
                case "ROLE_ADMIN" ->  updateOrganizationPassword(email,password);
                case "ROLE_ORGANIZATION" ->  updateAdminPassword(email,password);
            }
        }
    }

    private void updateUserPassword(String email, String password) {
        User user  = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    private void updateOrganizationPassword(String email, String password) {
        Organization organization  = organizationRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        organization.setPassword(passwordEncoder.encode(password));
        organizationRepository.save(organization);
    }

    private void updateAdminPassword(String email, String password) {
        Admin admin  = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        admin.setPassword(passwordEncoder.encode(password));
        adminRepository.save(admin);
    }

    private Optional<Verfication> verify(String Email, int code) {
        Optional<Verfication> verification = verificationRepository.findByEmail(Email);
        try {
            if (verification.isPresent()) {
                int verifiedCode = verification.get().getCode();
                if (verifiedCode == code) {
                    return verification;
                } else {
                    return Optional.empty();
                }
            } else {
                throw new RuntimeException("email not found in DB");
            }
        } catch (Exception e) {
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
            e.printStackTrace();
            return false;
        }
    }

    @Transactional
    public boolean forgetPasswordVerification(String Email, int code) {
        Optional<Verfication> verification=verify(Email, code);
        return verification.isPresent();
    }

    @Transactional
    public VerificationResponseDTO verifyEmail(VerificationDTO verificationDTO) {
        try {
            String email = verificationDTO.getEmail();
            int code = verificationDTO.getCode();
            Optional<Verfication> verification = verify(email, code);

            if (verification.isPresent()) {
                String password = verification.get().getPassword();
                String role = verification.get().getRole();

                Authenticatable account = switch (role) {
                    case "ORGANIZATION" -> organizationService.addOrganization(email, password);
                    case "USER" -> userService.addUser(email, password);
                    default -> null;
                };

                verificationRepository.delete(verification.get());

                if (account != null) {
                    String token = jwtTokenProvider.generateToken(account);

                    return VerificationResponseDTO.builder()
                            .success(true)
                            .message("Verification successful")
                            .token(token)
                            .id(account.getId())
                            .email(account.getEmail())
                            .role(account.getRole())
                            .build();
                }
            }

            return VerificationResponseDTO.builder()
                    .success(false)
                    .message("Invalid or expired code")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return VerificationResponseDTO.builder()
                    .success(false)
                    .message("Verification failed: " + e.getMessage())
                    .build();
        }
    }

    @Transactional
    public void deleteOldVerifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        verificationRepository.deleteOlderThan(cutoff);
    }
}