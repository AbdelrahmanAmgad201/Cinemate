package org.example.backend.verification;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationRepository extends JpaRepository<Verification, String> {
    Optional<Verification> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM Verification v WHERE v.createdAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
