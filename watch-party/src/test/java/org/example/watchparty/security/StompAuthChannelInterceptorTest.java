package org.example.watchparty.security;

import org.example.watchparty.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    private StompHeaderAccessor accessor;

    @BeforeEach
    void setUp() {
    }

    private Message<?> createMessage(StompCommand command) {
        accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void preSend_ConnectFrame_ValidToken_SetsUserPrincipal() {
        Message<?> message = createMessage(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer valid.token");

        Jwt jwt = Jwt.withTokenValue("valid.token")
                .header("alg", "RS256")
                .claim("id", 1L)
                .claim("name", "Test User")
                .claim("role", "ROLE_USER")
                .build();

        when(jwtDecoder.decode("valid.token")).thenReturn(jwt);

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
        assertNotNull(accessor.getUser());
        assertTrue(accessor.getUser() instanceof WatchPartyPrincipal);
        WatchPartyPrincipal principal = (WatchPartyPrincipal) accessor.getUser();
        assertEquals(1L, principal.userId());
        assertEquals("Test User", principal.userName());
        assertEquals("ROLE_USER", principal.role());
    }

    @Test
    void preSend_ConnectFrame_MissingAuthorizationHeader_ThrowsMessagingException() {
        Message<?> message = createMessage(StompCommand.CONNECT);

        MessagingException ex = assertThrows(MessagingException.class, () ->
                interceptor.preSend(message, null));
        assertTrue(ex.getMessage().contains("Missing or malformed Authorization header"));
    }

    @Test
    void preSend_ConnectFrame_InvalidToken_ThrowsMessagingException() {
        Message<?> message = createMessage(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer invalid.token");

        when(jwtDecoder.decode("invalid.token")).thenThrow(new JwtException("Invalid"));

        MessagingException ex = assertThrows(MessagingException.class, () ->
                interceptor.preSend(message, null));
        assertTrue(ex.getMessage().contains("Invalid access token"));
    }

    @Test
    void preSend_SubscribeFrame_AsMember_Passes() {
        Message<?> message = createMessage(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/watch-party/test-party");
        accessor.setUser(new WatchPartyPrincipal(1L, "User", "ROLE_USER"));

        when(redisService.isMemberOfSet("party:test-party:members", "user:1")).thenReturn(true);

        Message<?> result = interceptor.preSend(message, null);
        assertSame(message, result);
    }

    @Test
    void preSend_SubscribeFrame_AsNonMember_ThrowsMessagingException() {
        Message<?> message = createMessage(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/watch-party/test-party");
        accessor.setUser(new WatchPartyPrincipal(1L, "User", "ROLE_USER"));

        when(redisService.isMemberOfSet("party:test-party:members", "user:1")).thenReturn(false);

        MessagingException ex = assertThrows(MessagingException.class, () ->
                interceptor.preSend(message, null));
        assertTrue(ex.getMessage().contains("not a member"));
    }

    @Test
    void preSend_SendFrame_AsMember_Passes() {
        Message<?> message = createMessage(StompCommand.SEND);
        accessor.setDestination("/app/watch-party/test-party/chat");
        accessor.setUser(new WatchPartyPrincipal(1L, "User", "ROLE_USER"));

        when(redisService.isMemberOfSet("party:test-party:members", "user:1")).thenReturn(true);

        Message<?> result = interceptor.preSend(message, null);
        assertSame(message, result);
    }

    @Test
    void preSend_SendFrame_AsNonMember_ThrowsMessagingException() {
        Message<?> message = createMessage(StompCommand.SEND);
        accessor.setDestination("/app/watch-party/test-party/control");
        accessor.setUser(new WatchPartyPrincipal(1L, "User", "ROLE_USER"));

        when(redisService.isMemberOfSet("party:test-party:members", "user:1")).thenReturn(false);

        MessagingException ex = assertThrows(MessagingException.class, () ->
                interceptor.preSend(message, null));
        assertTrue(ex.getMessage().contains("not a member"));
    }

    @Test
    void preSend_DisconnectFrame_NoAuthorizationCheck() {
        Message<?> message = createMessage(StompCommand.DISCONNECT);
        Message<?> result = interceptor.preSend(message, null);
        assertSame(message, result);
        verifyNoInteractions(jwtDecoder, redisService);
    }

    @Test
    void preSend_HeartbeatFrame_NoAuthorizationCheck() {
        Message<?> message = MessageBuilder.withPayload(new byte[0]).build();

        Message<?> result = interceptor.preSend(message, null);
        assertSame(message, result);
        verifyNoInteractions(jwtDecoder, redisService);
    }
}
