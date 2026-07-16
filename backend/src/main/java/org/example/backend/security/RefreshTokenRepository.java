package org.example.backend.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Bulk delete by hash, returning the row count. This is the serialization point for
     * single-use rotation: of two concurrent rotations of the same token, exactly one
     * sees count == 1 (the analogue of the previous Redis GETDEL guarantee).
     */
    @Modifying
    @Query("delete from RefreshToken r where r.tokenHash = :hash")
    int deleteByHash(@Param("hash") String hash);

    /** Scheduled cleanup of expired rows. */
    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
