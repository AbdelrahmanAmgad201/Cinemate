package org.example.backend.watchparty;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.watchparty.DTOs.WatchPartyCreatedResponse;
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
    public ResponseEntity<WatchPartyCreatedResponse> createParty(
            HttpServletRequest request,
            @PathVariable Long movieId
    ) {
        WatchPartyUserDTO dto = WatchPartyUserDTO.builder()
                .userName((String) request.getAttribute("userName"))
                .userId((Long) request.getAttribute("userId"))
                .build();

        log.debug("Creating watch party for user: {}", dto);

        WatchPartyCreatedResponse response = watchPartyService.create(dto, movieId);
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
        WatchPartyDetailsResponse response = watchPartyService.get(partyId);
        return ResponseEntity.ok(response);
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

        WatchPartyDetailsResponse response = watchPartyService.join(dto, partyId);
        return ResponseEntity.ok(response);
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
        watchPartyService.leave(userId, partyId);
        return ResponseEntity.ok().build();
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
        watchPartyService.delete(userId, partyId);
        return ResponseEntity.noContent().build();
    }
}