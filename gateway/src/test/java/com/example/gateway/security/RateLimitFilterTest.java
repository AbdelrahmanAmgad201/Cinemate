package com.example.gateway.security;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private ObjectProvider<ProxyManager<String>> proxyManagerProvider;

    @Mock
    private ProxyManager<String> proxyManager;

    @Mock
    private RemoteBucketBuilder<String> remoteBucketBuilder;

    @Mock
    private BucketProxy bucketProxy;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        when(proxyManagerProvider.getObject()).thenReturn(proxyManager);
        rateLimitFilter = new RateLimitFilter(proxyManagerProvider);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void requestWithinLimit_Proceeds() throws ServletException, IOException {
        request.setRequestURI("/api/movie/1");
        request.setRemoteAddr("127.0.0.1");

        ConsumptionProbe probe = ConsumptionProbe.consumed(9, 0);

        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        when(remoteBucketBuilder.build(eq("rl:ip:127.0.0.1"), any(java.util.function.Supplier.class))).thenReturn(bucketProxy);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("9", response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    void requestExceedingLimit_Returns429WithJsonBody() throws ServletException, IOException {
        request.setRequestURI("/api/movie/1");
        request.setRemoteAddr("127.0.0.1");

        ConsumptionProbe probe = ConsumptionProbe.rejected(0, 5_000_000_000L, 0); // 5 seconds to refill

        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        when(remoteBucketBuilder.build(anyString(), any(java.util.function.Supplier.class))).thenReturn(bucketProxy);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(429, response.getStatus());
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
        assertEquals("5", response.getHeader("Retry-After")); // 5_000_000_000 ns = 5s
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"error\":\"Too many requests\"}", response.getContentAsString());
    }

    @Test
    void authenticatedUser_UsesDifferentBucketKeyThanAnonymous() throws ServletException, IOException {
        request.setRequestURI("/api/movie/1");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader(GatewayJwtAuthenticationFilter.H_ID, "42"); // X-User-Id

        ConsumptionProbe probe = ConsumptionProbe.consumed(39, 0);

        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(remoteBucketBuilder.build(keyCaptor.capture(), any(java.util.function.Supplier.class))).thenReturn(bucketProxy);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("rl:user:42", keyCaptor.getValue());
    }

    @Test
    void authEndpoint_UsesAuthBucketKey() throws ServletException, IOException {
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("192.168.1.1");

        ConsumptionProbe probe = ConsumptionProbe.consumed(9, 0);

        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(remoteBucketBuilder.build(keyCaptor.capture(), any(java.util.function.Supplier.class))).thenReturn(bucketProxy);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("rl:auth:ip:192.168.1.1", keyCaptor.getValue());
    }
}
