package org.example.backend.security;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Short-lived, single-use OAuth redirect handoff code (was a Redis key; see
 * OAuthExchangeService). Stores the JWT under a random code for ~30s so the frontend's
 * redirect page can trade the code for the real token without the token touching the URL.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "oauth_exchange_codes")
public class OAuthExchangeCode {

    @Id
    private UUID code;

    @Column(nullable = false, columnDefinition = "text")
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
