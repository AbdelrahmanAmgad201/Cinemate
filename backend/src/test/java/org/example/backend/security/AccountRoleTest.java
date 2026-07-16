package org.example.backend.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountRoleTest {

    @Test
    void fromString_acceptsBareRoleName() {
        assertEquals(AccountRole.USER, AccountRole.fromString("USER"));
        assertEquals(AccountRole.ADMIN, AccountRole.fromString("admin"));
        assertEquals(AccountRole.ORGANIZATION, AccountRole.fromString("Organization"));
    }

    @Test
    void fromString_acceptsRolePrefixedName() {
        assertEquals(AccountRole.USER, AccountRole.fromString("ROLE_USER"));
        assertEquals(AccountRole.ADMIN, AccountRole.fromString("role_admin"));
        assertEquals(AccountRole.ORGANIZATION, AccountRole.fromString("ROLE_ORGANIZATION"));
    }

    @Test
    void fromString_returnsNullForUnknownOrMissingRole() {
        assertNull(AccountRole.fromString("UNKNOWN"));
        assertNull(AccountRole.fromString(null));
    }

    @Test
    void authority_addsRolePrefix() {
        assertEquals("ROLE_USER", AccountRole.USER.authority());
        assertEquals("ROLE_ADMIN", AccountRole.ADMIN.authority());
        assertEquals("ROLE_ORGANIZATION", AccountRole.ORGANIZATION.authority());
    }
}
