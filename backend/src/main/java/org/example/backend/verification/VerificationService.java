package org.example.backend.verification;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.admin.AdminService;
import org.example.backend.security.AccountRegistry;
import org.example.backend.security.AccountRole;
import org.example.backend.security.Authenticatable;
import org.example.backend.security.JWTProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.backend.organization.OrganizationService;
import org.example.backend.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import lombok.Getter;
import lombok.Setter;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
@Service
public class VerificationService {

    private final VerificationRepository verificationRepository;

    private final AccountRegistry accountRegistry;

    private final OrganizationService organizationService;

    private final AdminService adminService;

    private final JWTProvider jwtTokenProvider;

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    private final RestTemplate restTemplate; // injected from AppConfig (has configured timeouts)
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final Clock clock;
    private final Random random = new Random();

    @Transactional
    public Verification forgetPassword(String email,String role) {
        int code = 100000 + random.nextInt(900000);
        if(sendVerificationEmail(email, code)){
            return addVerification(email ,code,role);
        }
        else{
            // Fail loudly (REL-05) instead of returning an empty, unpersisted object —
            // consistent with how UserService.signUp() handles the same failure.
            throw new RuntimeException(
                "Failed to send password reset email to " + email + ". Please try again later.");
        }
    }

    @Transactional
    public Verification addVerification(String email, String password, int code, String role) {
        String hashedPassword = passwordEncoder.encode(password);
        Optional<Verification> oldVerification = verificationRepository.findByEmail(email);
        oldVerification.ifPresent(verificationRepository::delete);
        Verification verification = Verification.builder()
                .email(email)
                .password(hashedPassword)
                .code(passwordEncoder.encode(String.valueOf(code)))
                .role(role)
                .build();
        return verificationRepository.save(verification);
    }

    private Verification addVerification(String email, int code,String role) {
        Optional<Verification> oldVerification = verificationRepository.findByEmail(email);
        oldVerification.ifPresent(verificationRepository::delete);
        Verification verification = Verification.builder()
                .email(email)
                .code(passwordEncoder.encode(String.valueOf(code)))
                .role(role)
                .build();
        return verificationRepository.save(verification);
    }

    @Transactional
    public void updatePasswordByVerificationCode(String email, UpdatePasswordWithVerificationDTO updatePasswordWithVerificationDTO,String role) {
        int verificationCode = updatePasswordWithVerificationDTO.getCode();
        String password = updatePasswordWithVerificationDTO.getPassword();
        Optional<Verification> verification =verify(email,verificationCode);
        if(verification.isPresent()) {
            accountRegistry.updatePassword(email, role, passwordEncoder.encode(password));
            // One-time code must be consumed on use (REL-04) — without this, the same
            // code can reset the password again for the rest of its 10-minute window.
            verificationRepository.delete(verification.get());
        }
    }

    private Optional<Verification> verify(String Email, int code) {
        Optional<Verification> verification = verificationRepository.findByEmail(Email);
        try {
            if (verification.isPresent()) {
                if (passwordEncoder.matches(String.valueOf(code), verification.get().getCode())) {
                    return verification;
                } else {
                    return Optional.empty();
                }
            } else {
                throw new RuntimeException("email not found in DB");
            }
        } catch (Exception e) {
            log.error("Verification check failed for email {}", Email, e);
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
            log.error("Failed to send verification email to {}", toEmail, e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean forgetPasswordVerification(String Email, int code) {
        Optional<Verification> verification=verify(Email, code);
        return verification.isPresent();
    }

    @Transactional
    public VerificationResponseDTO verifyEmail(VerificationDTO verificationDTO) {
        try {
            String email = verificationDTO.getEmail();
            int code = verificationDTO.getCode();
            Optional<Verification> verification = verify(email, code);

            if (verification.isPresent()) {
                String password = verification.get().getPassword();
                AccountRole role = AccountRole.fromString(verification.get().getRole());

                // Admin accounts aren't self-registered through email verification (they're
                // created directly via AdminService.addAdmin), so ADMIN falls through to null
                // here same as an unrecognized role.
                Authenticatable account = role == null ? null : switch (role) {
                    case ORGANIZATION -> organizationService.addOrganizationWithHashedPassword(email, password);
                    // Built directly against userRepository rather than via UserService, which
                    // avoids a UserService <-> VerificationService circular bean dependency.
                    case USER -> userRepository.save(User.builder().email(email).password(password).build());
                    case ADMIN -> null;
                };

                verificationRepository.delete(verification.get());

                if (account != null) {
                    String accessToken = jwtTokenProvider.generateAccessToken(account);

                    return VerificationResponseDTO.builder()
                            .success(true)
                            .message("Verification successful")
                            .accessToken(accessToken)
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
            log.error("Email verification failed", e);
            return VerificationResponseDTO.builder()
                    .success(false)
                    .message("Verification failed: " + e.getMessage())
                    .build();
        }
    }

    @Transactional
    public void deleteOldVerifications() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusMinutes(10);
        verificationRepository.deleteOlderThan(cutoff);
    }
}