package org.example.backend.security;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Server-side record of an opaque refresh token (was a Redis key; see RefreshTokenService).
 * Only the SHA-256 hash of the raw token is stored, so a DB dump yields no usable tokens.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "token_hash")
    private String tokenHash;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
