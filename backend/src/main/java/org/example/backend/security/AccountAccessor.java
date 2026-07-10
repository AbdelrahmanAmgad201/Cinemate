package org.example.backend.security;

import java.util.Optional;

/**
 * One implementation per {@link AccountRole}, each wrapping that role's own JPA repository.
 * Adding a new role means writing one new implementation and registering it in
 * {@link AccountRegistry} — not editing a switch statement buried in an unrelated service.
 */
public interface AccountAccessor {

    AccountRole role();

    Optional<Authenticatable> findByEmail(String email);

    /** Throws if no account with this email exists for this role. */
    void updatePassword(String email, String encodedPassword);
}
