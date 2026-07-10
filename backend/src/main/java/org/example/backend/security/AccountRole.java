package org.example.backend.security;

/**
 * The three account kinds Cinemate authenticates. Centralizes the string-normalization
 * that used to be duplicated (and inconsistently applied) across {@link AuthenticationService}
 * and {@code VerificationService} — some call sites passed "USER", others "ROLE_USER".
 */
public enum AccountRole {
    USER, ADMIN, ORGANIZATION;

    /** The exact string stored in JWTs / used by Spring Security's {@code hasAuthority(...)}. */
    public String authority() {
        return "ROLE_" + name();
    }

    /** Accepts "USER", "ROLE_USER", "user", etc. Returns null if unrecognized. */
    public static AccountRole fromString(String value) {
        if (value == null) return null;

        String normalized = value.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }

        try {
            return AccountRole.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
