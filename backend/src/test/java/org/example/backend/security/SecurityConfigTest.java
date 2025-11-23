package org.example.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    void testJwtAuthenticationFilterBean() {
        assertNotNull(securityConfig.jwtAuthenticationFilter());
    }

    @Test
    void testPasswordEncoderBean() {
        assertNotNull(securityConfig.passwordEncoder());
    }

    @Test
    void testCorsConfigurationSourceBean() {
        assertNotNull(securityConfig.corsConfigurationSource());
    }
}
