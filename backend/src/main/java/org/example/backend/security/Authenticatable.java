package org.example.backend.security;

public interface Authenticatable {
    Long getId();
    String getEmail();
    String getName();
    // String getPassword();
    String getRole();  // "ROLE_USER", "ROLE_ADMIN", "ROLE_ORGANIZATION"
    
    default Boolean getProfileComplete(){
        return true;
    }

    default String getPassword(){
        return null;
    }
}