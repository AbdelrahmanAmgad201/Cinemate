package org.example.backend.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.bson.types.ObjectId;
import org.example.backend.verification.Verfication;
import org.example.backend.verification.VerificationService;
import lombok.RequiredArgsConstructor;
import org.example.backend.security.CredentialsRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final Random random = new Random();
    @Autowired
    private VerificationService verificationService;


    public User addUser(String email, String password) {
        User user = User.builder()
                .email(email)
                .password(password)
                .build();
       return userRepository.save(user);
    }


    @Transactional
    public String setUserData(Long userId, UserDataDTO userDataDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
    public Verfication signUp(CredentialsRequest credentialsRequest) {
        String email = credentialsRequest.getEmail();
        String password = credentialsRequest.getPassword();
        String role = credentialsRequest.getRole();
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            throw new UserAlreadyExistsException(email);
        }
        int code = 100000 + random.nextInt(900000);
        if(verificationService.sendVerificationEmail(email, code)){
            return verificationService.addVerfication(email, password,code,role);
        }
        else{
            return new Verfication();
        }
    }

    public String getUserNameFromObjectUserId(ObjectId objectUserId) {
        Long userId = objectIdToLong(objectUserId);
        return getUserName(userId);
    }

    public String getUserName(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            if(user.get().getLastName()==null)
                return user.get().getFirstName();
            return user.get().getFirstName()+" "+user.get().getLastName();
        }
        else  {
            return "Unknown user";
        }
    }

    private Long objectIdToLong(ObjectId objectId) {
        String hex = objectId.toHexString();
        return new BigInteger(hex, 16).longValue();
    }
}
