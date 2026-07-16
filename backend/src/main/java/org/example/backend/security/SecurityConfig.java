package org.example.backend.security;

import lombok.RequiredArgsConstructor;
import org.example.backend.errorHandler.CustomAccessDeniedHandler;
import org.example.backend.errorHandler.CustomAuthEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.context.annotation.Lazy;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthEntryPoint authEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    @Lazy
    private final OAuthSuccessHandler oAuthSuccessHandler;

    @Bean
    public GatewayAuthenticationFilter gatewayAuthenticationFilter() {
        return new GatewayAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS is handled at the gateway now (single origin); the backend is
                // internal-only and never reached cross-origin by a browser.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/verification/**").permitAll()
                .requestMatchers("/api/user/v1/sign-up").permitAll()
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/organization/**").hasAuthority("ROLE_ORGANIZATION")
                .requestMatchers("/api/user/**").hasAuthority("ROLE_USER")
                .requestMatchers("/api/movie/**").permitAll()
                .requestMatchers("/api/movie-review/**").hasAuthority("ROLE_USER")
                .requestMatchers("/api/watch-history/**").hasAuthority("ROLE_USER")
                .requestMatchers("/api/liked-movie/**").hasAuthority("ROLE_USER")
                .requestMatchers("/api/watch-later/**").hasAuthority("ROLE_USER")
                .requestMatchers("/api/forum/**").hasAuthority("ROLE_USER")
                .requestMatchers("/api/comment/**").hasAuthority("ROLE_USER")
                .requestMatchers("/api/vote/**").hasAuthority("ROLE_USER")
                .requestMatchers("/api/forum-follow/**").hasAuthority("ROLE_USER")
                .requestMatchers("/api/feed/**").hasAuthority("ROLE_USER")
                .requestMatchers("/api/post/**").hasAuthority("ROLE_USER")
                // /api/watch-party/** is owned by the watch-party microservice now (Stage 1);
                // the gateway routes it there and enforces ROLE_USER at the edge. The backend
                // no longer serves it.

                .anyRequest().authenticated()
        )
        .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
        )
        .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization ->  authorization.baseUri("/oauth2/authorize"))
                .redirectionEndpoint(redirection -> redirection.baseUri("/login/oauth2/code/*"))
                .successHandler(oAuthSuccessHandler)
        )
            .addFilterBefore((gatewayAuthenticationFilter()), UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}