package org.example.watchparty.security;

import java.security.Principal;

/**
 * The authenticated identity bound to a STOMP session, derived from the verified access
 * token on CONNECT (REL-08). Once bound, {@code WebSocketController} stamps chat/control
 * events from this — never from client-supplied frame fields — which closes the previous
 * impersonation hole where {@code userId}/{@code userName} were trusted verbatim.
 */
public record WatchPartyPrincipal(Long userId, String userName, String role) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
