package org.example.watchparty;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.watchparty.dtos.WatchPartyCreatedResponse;
import org.example.watchparty.dtos.WatchPartyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Public watch-party API. Reached directly through the gateway (Stage 1): the gateway
 * authenticates the access token and forwards a trusted identity as X-User-* headers,
 * which {@code GatewayAuthenticationFilter} exposes as request attributes. This service
 * now owns the whole domain — there is no backend proxy in front of it.
 */
@RestController
@RequestMapping("/api/watch-party")
@RequiredArgsConstructor
public class WatchPartyController {

    private final WatchPartyService watchPartyService;

    @PostMapping("/v1/{movieId}")
    public ResponseEntity<WatchPartyCreatedResponse> create(
            HttpServletRequest request,
            @PathVariable Long movieId) {
        Long userId = requireUserId(request);
        String userName = (String) request.getAttribute("userName");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(watchPartyService.create(userId, userName, movieId));
    }

    @GetMapping("/v1/{partyId}")
    public ResponseEntity<WatchPartyResponse> get(@PathVariable String partyId) {
        return ResponseEntity.ok(watchPartyService.get(partyId));
    }

    @PutMapping("/v1/{partyId}/members")
    public ResponseEntity<WatchPartyResponse> join(
            HttpServletRequest request,
            @PathVariable String partyId) {
        Long userId = requireUserId(request);
        String userName = (String) request.getAttribute("userName");
        return ResponseEntity.ok(watchPartyService.join(userId, userName, partyId));
    }

    @DeleteMapping("/v1/{partyId}/members")
    public ResponseEntity<Void> leave(
            HttpServletRequest request,
            @PathVariable String partyId) {
        watchPartyService.leave(requireUserId(request), partyId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/v1/{partyId}")
    public ResponseEntity<Void> delete(
            HttpServletRequest request,
            @PathVariable String partyId) {
        watchPartyService.delete(requireUserId(request), partyId);
        return ResponseEntity.noContent().build();
    }

    // Identity is injected by GatewayAuthenticationFilter from the gateway's verified
    // X-User-* headers. Its absence means the request didn't arrive through the
    // authenticated edge — reject rather than act anonymously.
    private Long requireUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return (Long) userId;
    }
}
