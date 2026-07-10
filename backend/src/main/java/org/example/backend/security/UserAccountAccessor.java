package org.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.example.backend.errorHandler.ResourceNotFoundException;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserAccountAccessor implements AccountAccessor {

    private final UserRepository userRepository;

    @Override
    public AccountRole role() {
        return AccountRole.USER;
    }

    @Override
    public Optional<Authenticatable> findByEmail(String email) {
        return userRepository.findByEmail(email).map(user -> (Authenticatable) user);
    }

    @Override
    public void updatePassword(String email, String encodedPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }
}
