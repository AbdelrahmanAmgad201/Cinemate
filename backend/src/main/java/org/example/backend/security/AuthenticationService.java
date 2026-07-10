package org.example.backend.security;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AccountRegistry accountRegistry;
    private final PasswordEncoder passwordEncoder;

    public Optional<Authenticatable> findByEmailAndRole(String email, String role) {
        return accountRegistry.findByEmailAndRole(email, role);
    }

    public Optional<Authenticatable> authenticate(String email, String password, String role) {
        return findByEmailAndRole(email, role)
                .filter(account -> passwordEncoder.matches(password, account.getPassword()));
    }

    @Transactional
    public void updatePassword(String email, UpdatePasswordDTO updatePasswordDTO, String role) {
        String oldPassword = updatePasswordDTO.getOldPassword();
        String newPassword = updatePasswordDTO.getNewPassword();

        Optional<Authenticatable> account = authenticate(email, oldPassword, role);
        if (account.isEmpty()) throw new WrongPasswordException("Invalid email or password");

        accountRegistry.updatePassword(email, role, passwordEncoder.encode(newPassword));
    }
}
