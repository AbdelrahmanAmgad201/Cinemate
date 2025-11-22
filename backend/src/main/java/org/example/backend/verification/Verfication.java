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
@Table(name = "verfications")
public class Verfication {
    @Id
    @Column(name="email")
    private String email;
    @Column(name = "password")
    private String password;
    @Column(name = "code")
    private int code;
    @Column(name="role")
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
