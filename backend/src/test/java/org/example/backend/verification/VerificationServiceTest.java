package org.example.backend.verification;

import org.example.backend.AbstractPostgresIntegrationTest;
import org.example.backend.BackendApplication;
import org.example.backend.organization.OrganizationRepository;
import org.example.backend.security.JWTProvider;
import org.example.backend.security.SecurityConfig;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {BackendApplication.class, SecurityConfig.class})
@Transactional
class VerificationServiceTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private VerificationRepository verificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JWTProvider jwtProvider;

    @BeforeEach
    void setup() {
        verificationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        verificationService.setSendGridApiKey("TEST_KEY");
        verificationService.setFromEmail("noreply@example.com");

        // Mock JWT token generation
        when(jwtProvider.generateAccessToken(any())).thenReturn("mock-jwt-token");
    }

    @Test
    void verifyEmail_WhenCodeCorrect_AddsUserAndDeletesVerification() {
        VerificationDTO dto = new VerificationDTO("test1@example.com", 999999);
        Verification stored = Verification.builder()
                .email("test1@example.com")
                .password("encrypted-pass")
                .code(passwordEncoder.encode("999999"))
                .role("USER")
                .build();

        verificationRepository.save(stored);

        VerificationResponseDTO response = verificationService.verifyEmail(dto);

        assertTrue(response.isSuccess());
        assertEquals("Verification successful", response.getMessage());
        assertNotNull(response.getAccessToken());
        assertEquals("test1@example.com", response.getEmail());
        assertEquals("ROLE_USER", response.getRole());
        assertTrue(userRepository.findByEmail("test1@example.com").isPresent());
        assertFalse(verificationRepository.findByEmail("test1@example.com").isPresent());
    }

    @Test
    void verifyEmailOrganization_WhenCodeCorrect_AddsOrganizationAndDeletesVerification() {
        VerificationDTO dto = new VerificationDTO("test2@example.com", 999999);
        Verification stored = Verification.builder()
                .email("test2@example.com")
                .password("encrypted-pass")
                .code(passwordEncoder.encode("999999"))
                .role("ORGANIZATION")
                .build();

        verificationRepository.save(stored);

        VerificationResponseDTO response = verificationService.verifyEmail(dto);

        assertTrue(response.isSuccess());
        assertEquals("Verification successful", response.getMessage());
        assertNotNull(response.getAccessToken());
        assertEquals("test2@example.com", response.getEmail());
        assertEquals("ROLE_ORGANIZATION", response.getRole());
        assertTrue(organizationRepository.findByEmail("test2@example.com").isPresent());
        assertFalse(verificationRepository.findByEmail("test2@example.com").isPresent());
    }

    @Test
    void addVerification() {
        verificationService.addVerification("test2@example.com", "encrypted-pass", 999999, "ORGANIZATION");
        assertTrue(verificationRepository.findByEmail("test2@example.com").isPresent());
    }

    @Test
    void verifyEmail_WhenCodeIncorrect_ReturnsFalse() {
        VerificationDTO dto = new VerificationDTO("test@example.com", 111111);

        Verification stored = Verification.builder()
                .email("test@example.com")
                .password("encrypted-pass")
                .code(passwordEncoder.encode("222222"))
                .role("USER")
                .build();

        verificationRepository.save(stored);

        VerificationResponseDTO response = verificationService.verifyEmail(dto);

        assertFalse(response.isSuccess());
        assertEquals("Invalid or expired code", response.getMessage());
        assertNull(response.getAccessToken());
        assertFalse(userRepository.findByEmail("test@example.com").isPresent());
        assertTrue(verificationRepository.findByEmail("test@example.com").isPresent());
    }

    @Test
    void verifyEmailOrganization_WhenCodeIncorrect_ReturnsFalse() {
        VerificationDTO dto = new VerificationDTO("test@example.com", 111111);

        Verification stored = Verification.builder()
                .email("test@example.com")
                .password("encrypted-pass")
                .code(passwordEncoder.encode("222222"))
                .role("ORGANIZATION")
                .build();

        verificationRepository.save(stored);

        VerificationResponseDTO response = verificationService.verifyEmail(dto);

        assertFalse(response.isSuccess());
        assertEquals("Invalid or expired code", response.getMessage());
        assertNull(response.getAccessToken());
        assertFalse(organizationRepository.findByEmail("test@example.com").isPresent());
        assertTrue(verificationRepository.findByEmail("test@example.com").isPresent());
    }

    @Test
    void verifyEmail_WhenEmailNotFound_ReturnsFalse() {
        VerificationDTO dto = new VerificationDTO("notfound@example.com", 123456);

        VerificationResponseDTO response = verificationService.verifyEmail(dto);

        assertFalse(response.isSuccess());
        assertFalse(userRepository.findByEmail("notfound@example.com").isPresent());
    }
}