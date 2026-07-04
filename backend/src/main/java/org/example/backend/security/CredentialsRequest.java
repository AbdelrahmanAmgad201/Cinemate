package org.example.backend.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CredentialsRequest {
    // No @Size(min=...) on password: this DTO is shared with login, and an existing
    // account's password could predate any length policy — a min-length check here
    // would lock those users out instead of just failing auth normally.
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String role;
}
