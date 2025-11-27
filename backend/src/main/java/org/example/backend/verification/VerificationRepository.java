package org.example.backend.verification;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationRepository extends JpaRepository<Verfication, String> {
    Optional<Verfication> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM Verfication v WHERE v.createdAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
