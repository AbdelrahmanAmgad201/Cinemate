package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Phase 1 (routing only): permit everything so the gateway is a pure reverse proxy
 * while the backend keeps enforcing auth exactly as before. Token validation and the
 * route/role matrix move to the gateway in Phase 2, which replaces this chain.
 *
 * <p>spring-boot-starter-security is on the classpath (needed for Phase 2), and its
 * default would lock every route to authenticated — so this explicit permit-all chain
 * is required even to proxy traffic in Phase 1.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
