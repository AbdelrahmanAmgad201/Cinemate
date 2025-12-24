package org.example.backend.watchparty;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.watchparty.DTOs.WatchPartyDetailsResponse;
import org.example.backend.watchparty.DTOs.WatchPartyUserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/watch-party")
@RequiredArgsConstructor
public class WatchPartyController {
    private final WatchPartyService watchPartyService;

    /**
     * Creates a new watch party with the specified movie
     */
    @PostMapping("/{movieId}")
    public ResponseEntity<WatchParty> createParty(
            HttpServletRequest request,
            @PathVariable Long movieId
    ) {
        WatchPartyUserDTO dto = WatchPartyUserDTO.builder()
                .userName((String) request.getAttribute("userName"))
                .userId((Long) request.getAttribute("userId"))
                .build();

        System.out.println(dto);
        WatchParty response = watchPartyService.create(dto, movieId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Gets watch party details including all members
     */
    @GetMapping("/{partyId}")
    public ResponseEntity<WatchPartyDetailsResponse> getParty(
            HttpServletRequest request,
            @PathVariable String partyId
    ) {

        try {
            WatchPartyDetailsResponse response = watchPartyService.get(partyId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error getting party: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Joins an existing watch party using party ID
     */
    @PostMapping("/join/{partyId}")
    public ResponseEntity<WatchPartyDetailsResponse> joinParty(
            HttpServletRequest request,
            @PathVariable String partyId
    ) {
        WatchPartyUserDTO dto = WatchPartyUserDTO.builder()
                .userName((String) request.getAttribute("userName"))
                .userId((Long) request.getAttribute("userId"))
                .build();

        try {
            WatchPartyDetailsResponse response = watchPartyService.join(dto, partyId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error joining party: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Leaves a watch party
     * If the user is the host, the entire party is deleted
     */
    @DeleteMapping("/leave/{partyId}")
    public ResponseEntity<Void> leaveParty(
            HttpServletRequest request,
            @PathVariable String partyId
    ) {
        Long userId = (Long) request.getAttribute("userId");

        try {
            watchPartyService.leave(userId, partyId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error leaving party: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deletes a watch party (host only)
     */
    @DeleteMapping("/{partyId}")
    public ResponseEntity<Void> deleteParty(
            HttpServletRequest request,
            @PathVariable String partyId
    ) {
        Long userId = (Long) request.getAttribute("userId");

        try {
            watchPartyService.delete(userId, partyId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting party: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}