package org.example.backend.user;

import jakarta.transaction.Transactional;
import org.example.backend.verification.Verfication;
import org.example.backend.verification.VerificationService;
import lombok.RequiredArgsConstructor;
import org.example.backend.security.CredentialsRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final Random random = new Random();
    @Autowired
    private VerificationService verificationService;


    public void addUser(String email, String password) {
        User user = User.builder()
                .email(email)
                .password(password)
                .build();
        userRepository.save(user);
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
}
