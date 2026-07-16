package org.example.watchparty.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.watchparty.redis.RedisService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket authentication + authorization (REL-08), enforced on the STOMP client-inbound
 * channel before any frame reaches a controller:
 *
 * <ul>
 *   <li><b>CONNECT</b> — requires a valid {@code Authorization: Bearer <token>} header;
 *       verifies it and binds a {@link WatchPartyPrincipal} to the session. A missing or
 *       invalid token rejects the connection.</li>
 *   <li><b>SUBSCRIBE / SEND</b> — the destination must be a party topic/app path, and the
 *       authenticated user must be a member of that party (per the Redis member set).
 *       This stops anyone from subscribing to, or injecting control/chat into, a party
 *       they haven't joined — party-id guessing/scraping is no longer sufficient.</li>
 * </ul>
 *
 * <p>Rejection is by thrown exception, which the broker turns into a STOMP ERROR frame and
 * refuses the frame/connection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final RedisService redisService;

    // Destinations the client is allowed to touch; capture group 1 is the party id.
    private static final Pattern SUBSCRIBE_DEST = Pattern.compile("^/topic/watch-party/([^/]+)$");
    private static final Pattern SEND_DEST = Pattern.compile("^/app/watch-party/([^/]+)/(?:control|chat)$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Use the mutable accessor already attached to the inbound message (NOT
        // StompHeaderAccessor.wrap, which copies) so setUser() on CONNECT actually
        // sticks to the session and propagates to later frames.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message; // heartbeats, transport frames — nothing to authorize
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorizeMembership(accessor, SUBSCRIBE_DEST);
            case SEND -> authorizeMembership(accessor, SEND_DEST);
            default -> { /* DISCONNECT, ACK, etc. need no checks */ }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Missing or malformed Authorization header on CONNECT");
        }
        try {
            Jwt jwt = jwtDecoder.decode(header.substring(7)); // verifies RS256 signature + expiry
            Long userId = ((Number) jwt.getClaim("id")).longValue();
            String userName = jwt.getClaimAsString("name");
            String role = jwt.getClaimAsString("role");
            accessor.setUser(new WatchPartyPrincipal(userId, userName, role));
            log.debug("WebSocket authenticated: user {} ({})", userId, userName);
        } catch (Exception e) {
            throw new MessagingException("Invalid access token on CONNECT", e);
        }
    }

    private void authorizeMembership(StompHeaderAccessor accessor, Pattern allowedDestination) {
        Principal principal = accessor.getUser();
        if (!(principal instanceof WatchPartyPrincipal user)) {
            throw new MessagingException("Not authenticated");
        }

        String destination = accessor.getDestination();
        Matcher matcher = destination == null ? null : allowedDestination.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new MessagingException("Destination not allowed: " + destination);
        }

        String partyId = matcher.group(1);
        String membersKey = "party:" + partyId + ":members";
        String memberKey = "user:" + user.userId();
        if (Boolean.FALSE.equals(redisService.isMemberOfSet(membersKey, memberKey))) {
            throw new MessagingException(
                    "User " + user.userId() + " is not a member of party " + partyId);
        }
    }
}
