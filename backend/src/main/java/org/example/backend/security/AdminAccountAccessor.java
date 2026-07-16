package org.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.example.backend.admin.Admin;
import org.example.backend.admin.AdminRepository;
import org.example.backend.errorHandler.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class AdminAccountAccessor implements AccountAccessor {

    private final AdminRepository adminRepository;

    @Override
    public AccountRole role() {
        return AccountRole.ADMIN;
    }

    @Override
    public Optional<Authenticatable> findByEmail(String email) {
        return adminRepository.findByEmail(email).map(admin -> (Authenticatable) admin);
    }

    @Override
    public void updatePassword(String email, String encodedPassword) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        admin.setPassword(encodedPassword);
        adminRepository.save(admin);
    }
}
