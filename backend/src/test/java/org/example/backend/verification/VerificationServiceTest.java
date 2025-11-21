package org.example.backend.verification;

import org.example.backend.organization.OrganizationRepository;
import org.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VerificationServiceTest {

    @Autowired
    private VerificationRepository verificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void setup() {
        verificationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        verificationService.setSendGridApiKey("TEST_KEY");
        verificationService.setFromEmail("noreply@example.com");
    }
    @Test
    void verifyEmail_WhenCodeCorrect_AddsUserAndDeletesVerification() {
        VerificationDTO dto = new VerificationDTO("test1@example.com", 999999);
        Verfication stored = Verfication.builder()
                .email("test1@example.com")
                .password("encrypted-pass")
                .code(999999)
                .role("USER")
                .build();

        verificationRepository.save(stored);
        boolean result = verificationService.verifyEmail(dto);
        assertTrue(result);
        assertTrue(userRepository.findByEmail("test1@example.com").isPresent());
        assertFalse(verificationRepository.findByEmail("test1@example.com").isPresent());
    }

    @Test
    void verifyEmailOrganization_WhenCodeCorrect_AddsUserAndDeletesVerification() {
        VerificationDTO dto = new VerificationDTO("test2@example.com", 999999);
        Verfication stored = Verfication.builder()
                .email("test2@example.com")
                .password("encrypted-pass")
                .code(999999)
                .role("ORGANIZATION")
                .build();

        verificationRepository.save(stored);
        boolean result = verificationService.verifyEmail(dto);
        assertTrue(result);
        assertTrue(organizationRepository.findByEmail("test2@example.com").isPresent());
        assertFalse(verificationRepository.findByEmail("test2@example.com").isPresent());
    }


    // -------------------------------------------------
    // TEST: verifyEmail wrong code
    // -------------------------------------------------
    @Test
    void verifyEmail_WhenCodeIncorrect_ReturnsFalse() {
        VerificationDTO dto = new VerificationDTO("test@example.com", 111111);

        Verfication stored = Verfication.builder()
                .email("test@example.com")
                .password("encrypted-pass")
                .code(222222)
                .role("USER")
                .build();

        verificationRepository.save(stored);

        boolean result = verificationService.verifyEmail(dto);

        assertFalse(result);
        assertFalse(organizationRepository.findByEmail("test@example.com").isPresent());
        assertTrue(verificationRepository.findByEmail("test@example.com").isPresent());
    }

    // -------------------------------------------------
    // TEST: verifyEmail wrong code
    // -------------------------------------------------
    @Test
    void verifyEmailOrganization_WhenCodeIncorrect_ReturnsFalse() {
        VerificationDTO dto = new VerificationDTO("test@example.com", 111111);

        Verfication stored = Verfication.builder()
                .email("test@example.com")
                .password("encrypted-pass")
                .code(222222)
                .role("ORGANIZATION")
                .build();

        verificationRepository.save(stored);

        boolean result = verificationService.verifyEmail(dto);

        assertFalse(result);
        assertFalse(userRepository.findByEmail("test@example.com").isPresent());
        assertTrue(verificationRepository.findByEmail("test@example.com").isPresent());
    }

    // -------------------------------------------------
    // TEST: verifyEmail email not found
    // -------------------------------------------------
    @Test
    void verifyEmail_WhenEmailNotFound_ReturnsFalse() {
        VerificationDTO dto = new VerificationDTO("notfound@example.com", 123456);

        boolean result = verificationService.verifyEmail(dto);

        assertFalse(result);
        assertFalse(userRepository.findByEmail("notfound@example.com").isPresent());
    }
}
