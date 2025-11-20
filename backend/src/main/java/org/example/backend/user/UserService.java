package org.example.backend.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.security.CredentialsRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private User addUser(String email, String password) {
        String hashedPassword = passwordEncoder.encode(password);
        User user = User.builder()
                .email(email)
                .password(hashedPassword)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User signUp(CredentialsRequest credentialsRequest) {
        String email = credentialsRequest.getEmail();
        String password = credentialsRequest.getPassword();

        userRepository.findByEmail(email).ifPresent(u -> {
            throw new UserAlreadyExistsException("User with this email already exists");
        });

        return addUser(email, password);
    }
}
