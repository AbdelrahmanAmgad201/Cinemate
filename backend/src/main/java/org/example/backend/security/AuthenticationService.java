package org.example.backend.security;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.admin.Admin;
import org.example.backend.admin.AdminRepository;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<Authenticatable> findByEmailAndRole(String email, String role) {
        String normalizedRole = normalizeRole(role);

        return switch (normalizedRole) {
            case "ROLE_USER" -> userRepository.findByEmail(email)
                    .map(user -> (Authenticatable) user);
            case "ROLE_ADMIN" -> adminRepository.findByEmail(email)
                    .map(admin -> (Authenticatable) admin);
            case "ROLE_ORGANIZATION" -> organizationRepository.findByEmail(email)
                    .map(org -> (Authenticatable) org);
            default -> Optional.empty();
        };
    }

    public Optional<Authenticatable> authenticate(String email, String password, String role) {
        Optional<Authenticatable> account = findByEmailAndRole(email, role);

        if (account.isPresent() && passwordEncoder.matches(password, account.get().getPassword())) {
            return account;
        }

        return Optional.empty();
    }

    @Transactional
    public void updatePassword(String email,UpdatePasswordDTO updatePasswordDTO,String role) {
        String oldPassword = updatePasswordDTO.getOldPassword();
        String newPassword = updatePasswordDTO.getNewPassword();
        Optional<Authenticatable> account = authenticate(email, oldPassword, role);
        if (account.isPresent()) {
            account.get().setPassword(passwordEncoder.encode(newPassword));
            switch (role) {
                case "ROLE_USER" -> userRepository.save((User)account.get());
                case "ROLE_ADMIN" ->  adminRepository.save((Admin)account.get());
                case "ROLE_ORGANIZATION" ->  organizationRepository.save((Organization)account.get());
            }
        }
    }


    private String normalizeRole(String role) {
        if (role == null) return null;

        role = role.trim().toUpperCase();

        if (!role.startsWith("ROLE_")) {
            return "ROLE_" + role;
        }

        return role;
    }
}