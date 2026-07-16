package org.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.example.backend.errorHandler.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Single point of role-to-account-store dispatch, replacing the switch statements that used
 * to be duplicated across {@link AuthenticationService} and {@code VerificationService}.
 * Spring collects every {@link AccountAccessor} bean automatically, so this class never needs
 * to change when a new role is added.
 */
@Service
public class AccountRegistry {

    private final Map<AccountRole, AccountAccessor> accessors;

    public AccountRegistry(List<AccountAccessor> accessors) {
        this.accessors = accessors.stream()
                .collect(Collectors.toMap(AccountAccessor::role, Function.identity()));
    }

    public Optional<Authenticatable> findByEmailAndRole(String email, String role) {
        AccountAccessor accessor = resolve(role);
        return accessor == null ? Optional.empty() : accessor.findByEmail(email);
    }

    public void updatePassword(String email, String role, String encodedPassword) {
        AccountAccessor accessor = resolve(role);
        if (accessor == null) {
            throw new ResourceNotFoundException("Unknown role: " + role);
        }
        accessor.updatePassword(email, encodedPassword);
    }

    private AccountAccessor resolve(String role) {
        AccountRole accountRole = AccountRole.fromString(role);
        return accountRole == null ? null : accessors.get(accountRole);
    }
}
