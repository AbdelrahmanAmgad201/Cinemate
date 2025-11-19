package org.example.backend.user;

import jakarta.transaction.Transactional;
import org.example.backend.security.CredentialsRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    private User addUser(String email, String password) {
        System.out.println(email);
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
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }
        return addUser(email, password);
    }
}