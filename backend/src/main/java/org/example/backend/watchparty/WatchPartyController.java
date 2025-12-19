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
        WatchPartyUserDTO dto = WatchPartyUserDTO.builder()
                .userName((String) request.getAttribute("userName"))
                .userId((Long) request.getAttribute("userId"))
                .build();

        WatchParty response = watchPartyService.create(dto, movieId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Gets watch party details
    @GetMapping("/{partyId}")
    public ResponseEntity<WatchPartyDetailsResponse> getParty(
            HttpServletRequest request,
            @PathVariable String partyId
    ) {
        WatchPartyUserDTO dto = WatchPartyUserDTO.builder()
                .userName((String) request.getAttribute("userName"))
                .userId((Long) request.getAttribute("userId"))
                .build();

        WatchPartyDetailsResponse response = watchPartyService.get(dto, partyId);
        return ResponseEntity.ok(response);
    }

    // Deletes party (host only)
    @DeleteMapping("/{partyId}")
    public ResponseEntity<Void> deleteParty(
            HttpServletRequest request,
            @PathVariable String partyId
    ) {
        Long userId = (Long) request.getAttribute("userId");
        watchPartyService.delete(userId, partyId);
        // return simple mssg that its done
        return ResponseEntity.noContent().build();
    }

    // Join party using code
    @PostMapping("/join/{partyId}")
    public ResponseEntity<WatchPartyDetailsResponse> joinWithInvite(
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

}