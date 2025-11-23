package org.example.backend.security;

import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class CredentialsRequest {
    private String email;
    private String password;
    private String role;
}
