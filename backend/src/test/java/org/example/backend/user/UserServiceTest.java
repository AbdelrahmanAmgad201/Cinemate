package org.example.backend.user;

import org.example.backend.BackendApplication;
import org.example.backend.security.CredentialsRequest;
import org.example.backend.security.SecurityConfig;
import org.example.backend.verification.Verfication;
import org.example.backend.verification.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {BackendApplication.class, SecurityConfig.class})
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        verificationService = mock(VerificationService.class);

        userService = new UserService(userRepository); // only 1 arg
        // manually inject
        ReflectionTestUtils.setField(userService, "verificationService", verificationService);
    }

    @Test
    void testSignUp_Successful() {
        // given
        CredentialsRequest request = new CredentialsRequest("test@example.com", "pass123", "USER");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        when(verificationService.sendVerificationEmail(eq("test@example.com"), anyInt()))
                .thenReturn(true);

        Verfication stored = new Verfication();
        when(verificationService.addVerfication(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(stored);

        // when
        Verfication result = userService.signUp(request);

        // then
        assertNotNull(result);
        assertSame(stored, result);

        verify(userRepository).findByEmail("test@example.com");
        verify(verificationService).sendVerificationEmail(eq("test@example.com"), anyInt());
        verify(verificationService).addVerfication(eq("test@example.com"), eq("pass123"), anyInt(), eq("USER"));
    }

    @Test
    void testSignUp_UserAlreadyExists() {
        // given
        CredentialsRequest request = new CredentialsRequest("test@example.com", "pass123", "USER");
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(new User()));

        // when + then
        assertThrows(UserAlreadyExistsException.class,
                () -> userService.signUp(request));

        verify(userRepository).findByEmail("test@example.com");
        verifyNoMoreInteractions(verificationService);
    }

    @Test
    void testSignUp_EmailSendingFails() {
        // given
        CredentialsRequest request = new CredentialsRequest("test@example.com", "pass123", "USER");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        when(verificationService.sendVerificationEmail(eq("test@example.com"), anyInt()))
                .thenReturn(false);

        // when
        Verfication result = userService.signUp(request);

        // then
        assertNotNull(result);
        // you return empty Verfication() when email fails
        assertNull(result.getEmail()); // depends on your class fields
        verify(verificationService).sendVerificationEmail(eq("test@example.com"), anyInt());
        verify(verificationService, never()).addVerfication(any(), any(), anyInt(), any());
    }
}
