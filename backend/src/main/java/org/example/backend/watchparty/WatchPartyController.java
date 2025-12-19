package org.example.backend.watchparty;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/watch-party")
@RequiredArgsConstructor
public class WatchPartyController {
    private final WatchPartyService watchPartyService;

    // Creates a watch party (websocket) with this movie and returns partyId
    @PostMapping("/{movieId}")
    public ResponseEntity<WatchParty> createParty(
            HttpServletRequest request,
            @PathVariable Long movieId
    ) {
        Long userId = (Long) request.getAttribute("userId");
        WatchParty response = watchPartyService.create(userId, movieId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Gets watch party details
    @GetMapping("/{partyId}")
    public ResponseEntity<WatchPartyDetailsResponse> getParty(
            HttpServletRequest request,
            @PathVariable String partyId
    ) {
        Long userId = (Long) request.getAttribute("userId");
        String name = (String) request.getAttribute("userName");
        // TODO: Validate if user is member in party

        WatchPartyDetailsResponse response = watchPartyService.get(userId, partyId);
        return ResponseEntity.ok(response);
    }

    // Deletes party (host only)
    @DeleteMapping("/{partyId}")
    public ResponseEntity<Void> deleteParty(
            HttpServletRequest request,
            @PathVariable String partyId
    ) {
        Long userId = (Long) request.getAttribute("userId");

        // TODO: Service will verify user is the host

        watchPartyService.delete(userId, partyId);
        return ResponseEntity.noContent().build();
    }

    // Join party using code
    @PostMapping("/join/{partyId}")
    public ResponseEntity<WatchPartyDetailsResponse> joinWithInvite(
            HttpServletRequest request,
            @PathVariable String partyId
    ) {
        Long userId = (Long) request.getAttribute("userId");
        String name = (String) request.getAttribute("userName");
        WatchPartyDetailsResponse response = watchPartyService.join(userId, partyId);
        return ResponseEntity.ok(response);
    }
}