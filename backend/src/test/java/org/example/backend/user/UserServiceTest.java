package org.example.backend.user;

import org.bson.types.ObjectId;
import org.example.backend.BackendApplication;
import org.example.backend.security.CredentialsRequest;
import org.example.backend.security.JWTProvider;
import org.example.backend.security.SecurityConfig;
import org.example.backend.verification.Verfication;
import org.example.backend.verification.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import com.mongodb.client.result.UpdateResult;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
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
    private JWTProvider jwtProvider;

    @Autowired
    private UserService userService;

    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mongoTemplate = mock(MongoTemplate.class);
        verificationService = mock(VerificationService.class);
        jwtProvider = mock(JWTProvider.class);

        userService = new UserService(userRepository, mongoTemplate);
        ReflectionTestUtils.setField(userService, "verificationService", verificationService);
        ReflectionTestUtils.setField(userService, "jwtProvider", jwtProvider);
    }

    // =============== SignUp Tests ===============
    @Test
    void testSignUp_Successful() {
        CredentialsRequest request = new CredentialsRequest("test@example.com", "pass123", "USER");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());
        when(verificationService.sendVerificationEmail(eq("test@example.com"), anyInt()))
                .thenReturn(true);

        Verfication stored = new Verfication();
        when(verificationService.addVerfication(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(stored);

        Verfication result = userService.signUp(request);

        assertNotNull(result);
        assertSame(stored, result);
        verify(userRepository).findByEmail("test@example.com");
        verify(verificationService).sendVerificationEmail(eq("test@example.com"), anyInt());
        verify(verificationService).addVerfication(eq("test@example.com"), eq("pass123"), anyInt(), eq("USER"));
    }

    @Test
    void testSignUp_UserAlreadyExists() {
        CredentialsRequest request = new CredentialsRequest("test@example.com", "pass123", "USER");
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExistsException.class,
                () -> userService.signUp(request));

        verify(userRepository).findByEmail("test@example.com");
        verifyNoMoreInteractions(verificationService);
    }

    @Test
    void testSignUp_EmailSendingFails() {
        CredentialsRequest request = new CredentialsRequest("test@example.com", "pass123", "USER");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());
        when(verificationService.sendVerificationEmail(eq("test@example.com"), anyInt()))
                .thenReturn(false);

        Verfication result = userService.signUp(request);

        assertNotNull(result);
        verify(verificationService).sendVerificationEmail(eq("test@example.com"), anyInt());
        verify(verificationService, never()).addVerfication(any(), any(), anyInt(), any());
    }

    // =============== AddUser Tests ===============
    @Test
    void testAddUser_Success() {
        String email = "new@example.com";
        String password = "password123";

        User savedUser = User.builder()
                .id(1L)
                .email(email)
                .password(password)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.addUser(email, password);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(password, result.getPassword());
        verify(userRepository).save(any(User.class));
    }

    // =============== SetUserData Tests ===============
    @Test
    void testSetUserData_Success() {
        Long userId = 1L;
        UserDataDTO dto = new UserDataDTO();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setAbout("About me");
        dto.setBirthday(LocalDate.of(1990, 1, 1));
        dto.setGender("MALE");

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        String result = userService.setUserData(userId, dto);

        assertEquals("User data updated successfully", result);
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("About me", user.getAbout());
        assertEquals(Gender.MALE, user.getGender());
        verify(userRepository).save(user);
    }

    @Test
    void testSetUserData_UserNotFound() {
        Long userId = 1L;
        UserDataDTO dto = new UserDataDTO();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.setUserData(userId, dto));
    }

    @Test
    void testSetUserData_WithNullGender() {
        Long userId = 1L;
        UserDataDTO dto = new UserDataDTO();
        dto.setFirstName("John");
        dto.setGender(null);

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        String result = userService.setUserData(userId, dto);

        assertEquals("User data updated successfully", result);
        assertNull(user.getGender());
    }

    // =============== CompleteProfile Tests ===============
    @Test
    void testCompleteProfile_Success() {
        Long userId = 1L;
        ProfileCompletionDTO dto = new ProfileCompletionDTO();
        dto.setBirthday(LocalDate.of(1990, 1, 1));
        dto.setGender("FEMALE");

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtProvider.generateToken(user)).thenReturn("jwt-token");

        String result = userService.completeProfile(userId, dto);

        assertEquals("jwt-token", result);
        assertEquals(Gender.FEMALE, user.getGender());
        assertTrue(user.getProfileComplete());
        verify(jwtProvider).generateToken(user);
    }

    @Test
    void testCompleteProfile_InvalidGender() {
        Long userId = 1L;
        ProfileCompletionDTO dto = new ProfileCompletionDTO();
        dto.setBirthday(LocalDate.of(1990, 1, 1));
        dto.setGender("INVALID");

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> userService.completeProfile(userId, dto));
    }

    // =============== IsPublic/SetIsPublic Tests ===============
    @Test
    void testIsPublic_ReturnsTrue() {
        Long userId = 1L;
        User user = new User();
        user.setIsPublic(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Boolean result = userService.isPublic(userId);

        assertTrue(result);
    }

    @Test
    void testSetIsPublic_ChangesValue() {
        Long userId = 1L;
        User user = new User();
        user.setIsPublic(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.setIsPublic(userId, true);

        assertTrue(user.getIsPublic());
        verify(userRepository).save(user);
    }

    @Test
    void testSetIsPublic_NoChangeWhenSameValue() {
        Long userId = 1L;
        User user = new User();
        user.setIsPublic(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.setIsPublic(userId, true);

        verify(userRepository, never()).save(any());
    }

    // =============== UpdateAbout Tests ===============
    @Test
    void testUpdateAbout_Success() {
        Long userId = 1L;
        AboutDTO dto = new AboutDTO();
        dto.setAbout("New about text");

        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateAbout(userId, dto);

        assertEquals("New about text", user.getAbout());
        verify(userRepository).save(user);
    }

    // =============== UpdateBirthDate Tests ===============
    @Test
    void testUpdateBirthDate_Success() {
        Long userId = 1L;
        LocalDate newDate = LocalDate.of(1995, 5, 15);
        BirthDateDTO dto = new BirthDateDTO();
        dto.setBirthDate(newDate);

        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateBirthDate(userId, dto);

        assertEquals(newDate, user.getBirthDate());
        verify(userRepository).save(user);
    }

    // =============== UpdateName Tests ===============
    @Test
    void testUpdateName_Success() {
        Long userId = 1L;
        UserName userName = new UserName();
        userName.setFirstName("Jane");
        userName.setLastName("Smith");

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Mock MongoDB operations
        when(mongoTemplate.findDistinct(any(Query.class), eq("_id"), eq("posts"), eq(ObjectId.class)))
                .thenReturn(Arrays.asList());

        userService.updateName(userId, userName);

        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateName_WithPostsUpdate() {
        Long userId = 1L;
        UserName userName = new UserName();
        userName.setFirstName("Jane");
        userName.setLastName("Smith");

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        List<ObjectId> postIds = Arrays.asList(
                new ObjectId(),
                new ObjectId()
        );

        when(mongoTemplate.findDistinct(any(Query.class), eq("_id"), eq("posts"), eq(ObjectId.class)))
                .thenReturn(postIds);

        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(2L);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq("posts")))
                .thenReturn(updateResult);

        userService.updateName(userId, userName);

        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
    }

    // =============== GetUserName Tests ===============
    @Test
    void testGetUserName_WithFullName() {
        Long userId = 1L;
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        String result = userService.getUserName(userId);

        assertEquals("John Doe", result);
    }

    @Test
    void testGetUserName_WithOnlyFirstName() {
        Long userId = 1L;
        User user = new User();
        user.setFirstName("John");
        user.setLastName(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        String result = userService.getUserName(userId);

        assertEquals("John", result);
    }

    @Test
    void testGetUserName_UserNotFound() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        String result = userService.getUserName(userId);

        assertEquals("Unknown user", result);
    }

    @Test
    void testGetUserNameFromObjectUserId() {
        ObjectId objectId = new ObjectId("000000000000000000000001");
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

        String result = userService.getUserNameFromObjectUserId(objectId);

        assertEquals("John Doe", result);
    }

    // =============== GetUserProfile Tests ===============
    @Test
    void testGetUserProfile_Success() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setAbout("About me");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setGender(Gender.MALE);
        user.setNumberOfFollowers(100);
        user.setNumberOfFollowing(50);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponseDTO result = userService.getUserProfile(userId);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals(100, result.getNumberOfFollowers());
        assertEquals(50, result.getNumberOfFollowing());
        assertEquals("About me", result.getAboutMe());
        assertEquals(Gender.MALE, result.getGender());
    }

    @Test
    void testGetUserProfile_UserNotFound() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.getUserProfile(userId));
    }
}