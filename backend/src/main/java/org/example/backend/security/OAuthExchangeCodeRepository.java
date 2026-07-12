package org.example.backend.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface OAuthExchangeCodeRepository extends JpaRepository<OAuthExchangeCode, UUID> {

    /** Serialization point for single-use redeem (see RefreshTokenRepository.deleteByHash). */
    @Modifying
    @Query("delete from OAuthExchangeCode c where c.code = :code")
    int deleteByCode(@Param("code") UUID code);

    /** Scheduled cleanup of expired codes. */
    @Modifying
    @Query("delete from OAuthExchangeCode c where c.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
