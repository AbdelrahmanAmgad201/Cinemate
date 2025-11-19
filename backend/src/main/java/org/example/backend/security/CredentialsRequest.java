package org.example.backend.security;

import lombok.Data;

@Data
public class CredentialsRequest {
    private String email;
    private String password;
    private String role;
}
