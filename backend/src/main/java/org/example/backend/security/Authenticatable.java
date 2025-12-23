package org.example.backend.security;

public interface Authenticatable {
    Long getId();
    String getEmail();
    // String getPassword();
    String getRole();  // "ROLE_USER", "ROLE_ADMIN", "ROLE_ORGANIZATION"

    default String getPassword(){
        return null;
    }
    void setPassword(String password);
}