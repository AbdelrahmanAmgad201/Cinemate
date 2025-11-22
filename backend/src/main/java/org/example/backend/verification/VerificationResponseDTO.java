package org.example.backend.verification;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerificationResponseDTO{
    private boolean success;
    private String message;
    private String token;
    private Long id;
    private String email;
    private String role;
}