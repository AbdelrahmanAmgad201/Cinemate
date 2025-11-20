package org.example.backend.user;

import jakarta.transaction.Transactional;
import org.example.backend.verification.Verfication;
import org.example.backend.verification.VerificationRepository;
import org.example.backend.verification.VerificationService;
import lombok.RequiredArgsConstructor;
import org.example.backend.security.CredentialsRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();
    private final VerificationRepository verificationRepository;
    @Autowired
    private VerificationService verificationService;

    private Verfication addVerfication(String email, String password, int code) {
        String hashedPassword = passwordEncoder.encode(password);
        Optional<Verfication> oldVerification = verificationRepository.findByEmail(email);
        if (oldVerification.isPresent()) {
            verificationRepository.delete(oldVerification.get());

        }
            Verfication verfication= Verfication.builder()
                    .email(email)
                    .password(hashedPassword)
                    .code(code)
                    .build();

        return verificationRepository.save(verfication);
    }

    @Transactional
    public Verfication signUp(CredentialsRequest credentialsRequest) {
        String email = credentialsRequest.getEmail();
        String password = credentialsRequest.getPassword();
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }
        int code = 100000 + random.nextInt(900000);
        if(verificationService.sendVerificationEmail(email, code)){
            return addVerfication(email, password,code);
        }
        else{
            return new Verfication();
        }
    }
}
