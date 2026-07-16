package org.example.backend.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The backend trusts {@code X-User-*} headers on the assumption the gateway is the
 * only thing that can reach it and strips any inbound copies before injecting its
 * own (see {@link GatewayAuthenticationFilter} javadoc). These tests only cover what
 * the filter does with whatever headers land on its doorstep -- they can't prove the
 * gateway upholds its side of that contract.
 */
class GatewayAuthenticationFilterTest {

    private final GatewayAuthenticationFilter filter = new GatewayAuthenticationFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void roleAndEmailPresent_PopulatesSecurityContextAndRequestAttributes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");
        request.addHeader("X-User-Role", "ROLE_USER");
        request.addHeader("X-User-Email", "jane@example.com");
        request.addHeader("X-User-Name", "Jane");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.getPrincipal()).isEqualTo("jane@example.com");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
        assertThat(request.getAttribute("userId")).isEqualTo(42L);
        assertThat(request.getAttribute("userRole")).isEqualTo("ROLE_USER");
        assertThat(request.getAttribute("userEmail")).isEqualTo("jane@example.com");
        assertThat(request.getAttribute("userName")).isEqualTo("Jane");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void roleHeaderMissing_LeavesRequestUnauthenticatedButStillForwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Email", "jane@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void emailHeaderMissing_LeavesRequestUnauthenticatedButStillForwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Role", "ROLE_USER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void blankRoleHeader_LeavesRequestUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Role", "   ");
        request.addHeader("X-User-Email", "jane@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void malformedUserIdHeader_StillAuthenticatesWithNullUserIdAttribute() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "not-a-number");
        request.addHeader("X-User-Role", "ROLE_ADMIN");
        request.addHeader("X-User-Email", "admin@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(request.getAttribute("userId")).isNull();
    }

    @Test
    void noHeadersAtAll_ForwardsRequestWithoutAuthenticating() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
