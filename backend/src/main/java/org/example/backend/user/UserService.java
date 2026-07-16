package org.example.backend.user;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.common.dto.AboutDTO;
import org.example.backend.errorHandler.ResourceNotFoundException;
import org.example.backend.verification.Verification;
import org.example.backend.verification.VerificationService;
import lombok.RequiredArgsConstructor;
import org.example.backend.security.CredentialsRequest;
import org.example.backend.security.JWTProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final Random random = new Random();
    private final VerificationService verificationService;
    private final JWTProvider jwtProvider;

    /**
     * Creates a user record. {@code password} MUST already be a BCrypt hash (REL-09).
     */
    public User addUserWithHashedPassword(String email, String hashedPassword) {
        User user = User.builder()
                .email(email)
                .password(hashedPassword)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public String updateUserData(Long userId, UserDataDTO userDataDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(userDataDTO.getFirstName());
        user.setLastName(userDataDTO.getLastName());
        user.setAbout(userDataDTO.getAbout());
        user.setBirthDate(userDataDTO.getBirthday());

        if (userDataDTO.getGender() != null) {
            user.setGender(Gender.valueOf(userDataDTO.getGender().toUpperCase()));
        }

        userRepository.save(user);
        return "User data updated successfully";
    }

    @Transactional
    public String completeProfile(Long userId, ProfileCompletionDTO profileData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setBirthDate(profileData.getBirthday());
        try {
            user.setGender(Gender.valueOf(profileData.getGender().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid gender value");
        }
        user.setProfileComplete(true);

        userRepository.save(user);

        // Re-issue the access token so the profileComplete claim flips to true immediately.
        return jwtProvider.generateAccessToken(user);
    }

    @Transactional
    public Verification signUp(CredentialsRequest credentialsRequest) {
        String email = credentialsRequest.getEmail();
        String password = credentialsRequest.getPassword();
        String role = credentialsRequest.getRole();
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            throw new UserAlreadyExistsException(email);
        }
        int code = 100000 + random.nextInt(900000);
        if (verificationService.sendVerificationEmail(email, code)) {
            return verificationService.addVerification(email, password, code, role);
        } else {
            throw new RuntimeException(
                    "Failed to send verification email to " + email + ". Please try again later.");
        }
    }

    public Boolean isPublic(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getIsPublic();
    }

    public void setIsPublic(Long userId, Boolean isPublic) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getIsPublic() == isPublic) return;
        user.setIsPublic(isPublic);
        userRepository.save(user);
    }

    @Transactional
    public void updateAbout(Long userId, AboutDTO aboutDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setAbout(aboutDTO.getAbout());
        userRepository.save(user);
    }

    @Transactional
    public void updateBirthDate(Long userId, BirthDateDTO birthDateDTO) {
        LocalDate birthdate = birthDateDTO.getBirthDate();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setBirthDate(birthdate);
        userRepository.save(user);
    }

    @Transactional
    public void updateName(Long userId, UserName userName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFirstName(userName.getFirstName());
        user.setLastName(userName.getLastName());
        userRepository.save(user);
        // No authorName denormalization to sync onto posts anymore — the name is joined.
    }

    public String getUserName(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            if (user.get().getLastName() == null)
                return user.get().getFirstName();
            return user.get().getFirstName() + " " + user.get().getLastName();
        } else {
            return "Unknown user";
        }
    }

    public UserProfileResponseDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserProfileResponseDTO.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .numberOfFollowers(user.getNumberOfFollowers())
                .numberOfFollowing(user.getNumberOfFollowing())
                .createdAt(user.getCreatedAt())
                .aboutMe(user.getAbout())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .build();
    }
}
