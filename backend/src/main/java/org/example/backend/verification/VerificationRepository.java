package org.example.backend.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationRepository extends JpaRepository<Verfication, String> {
    Optional<Verfication> findByEmail(String email);
}
