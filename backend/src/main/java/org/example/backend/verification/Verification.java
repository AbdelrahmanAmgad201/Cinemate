package org.example.backend.verification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "verifications")
public class Verification {
    @Id
    @Column(name="email")
    private String email;
    @Column(name = "password")
    private String password;
    // Stores a BCrypt hash of the code, not the raw code — see SEC-10.
    @Column(name = "code", length = 60)
    private String code;
    @Column(name="role",nullable = false)
    private String role;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
