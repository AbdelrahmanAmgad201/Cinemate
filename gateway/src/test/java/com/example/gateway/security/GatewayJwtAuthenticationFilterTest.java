package com.example.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayJwtAuthenticationFilterTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private FilterChain filterChain;

    private GatewayJwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new GatewayJwtAuthenticationFilter(jwtDecoder);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_SetsSecurityContextAndInjectsHeaders() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        Jwt mockJwt = Jwt.withTokenValue("valid.jwt.token")
                .header("alg", "RS256")
                .subject("10")
                .claim("id", 10L)
                .claim("role", "ROLE_USER")
                .claim("email", "user@example.com")
                .claim("name", "Test User")
                .build();

        when(jwtDecoder.decode("valid.jwt.token")).thenReturn(mockJwt);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("10", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        ArgumentCaptor<HttpServletRequest> reqCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(reqCaptor.capture(), eq(response));

        HttpServletRequest wrappedReq = reqCaptor.getValue();
        assertEquals("10", wrappedReq.getHeader("X-User-Id"));
        assertEquals("ROLE_USER", wrappedReq.getHeader("X-User-Role"));
        assertEquals("user@example.com", wrappedReq.getHeader("X-User-Email"));
        assertEquals("Test User", wrappedReq.getHeader("X-User-Name"));
    }

    @Test
    void validToken_InboundForgedXUserHeaders_AreStripped() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        request.addHeader("X-User-Role", "ROLE_ADMIN"); // Forged!

        Jwt mockJwt = Jwt.withTokenValue("valid.jwt.token")
                .header("alg", "RS256")
                .subject("10")
                .claim("id", 10L)
                .claim("role", "ROLE_USER")
                .build();

        when(jwtDecoder.decode("valid.jwt.token")).thenReturn(mockJwt);

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<HttpServletRequest> reqCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(reqCaptor.capture(), eq(response));

        HttpServletRequest wrappedReq = reqCaptor.getValue();
        assertEquals("ROLE_USER", wrappedReq.getHeader("X-User-Role"));
    }

    @Test
    void noAuthorizationHeader_RequestProceedsUnauthenticated() throws ServletException, IOException {
        request.addHeader("X-User-Role", "ROLE_ADMIN"); // Forged header to ensure it gets stripped

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        ArgumentCaptor<HttpServletRequest> reqCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(reqCaptor.capture(), eq(response));

        HttpServletRequest wrappedReq = reqCaptor.getValue();
        assertNull(wrappedReq.getHeader("X-User-Role")); // Stripped
    }

    @Test
    void bearerTokenPresentButJwtDecoderThrows_RequestProceedsUnauthenticated() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid.jwt.token");
        when(jwtDecoder.decode("invalid.jwt.token")).thenThrow(new JwtException("Invalid token"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        ArgumentCaptor<HttpServletRequest> reqCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(reqCaptor.capture(), eq(response));

        HttpServletRequest wrappedReq = reqCaptor.getValue();
        assertNull(wrappedReq.getHeader("X-User-Id"));
    }

    @Test
    void validToken_NoRoleClaim_AuthenticationHasEmptyAuthorities() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        Jwt mockJwt = Jwt.withTokenValue("valid.jwt.token")
                .header("alg", "RS256")
                .subject("10")
                .claim("id", 10L)
                .build();

        when(jwtDecoder.decode("valid.jwt.token")).thenReturn(mockJwt);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("10", auth.getPrincipal());
        assertTrue(auth.getAuthorities().isEmpty());
    }
}
