package org.example.backend.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SecurityConfigUnitTest {

    @Test
    void testBeansExist() {
        // Mock dependencies
        JWTProvider jwtProvider = mock(JWTProvider.class);

        SecurityConfig securityConfig = new SecurityConfig(jwtProvider);

        assertNotNull(securityConfig.passwordEncoder());
        assertNotNull(securityConfig.jwtAuthenticationFilter());
        assertNotNull(securityConfig.corsConfigurationSource());
    }
}
