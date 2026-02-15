package org.example.watchparty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.watchparty.dtos.UserDataDTO;
import org.example.watchparty.dtos.WatchParty;
import org.example.watchparty.dtos.WatchPartyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/watch-party")
@RequiredArgsConstructor
public class WatchPartyController {

    private final WatchPartyService watchPartyService;

    @PostMapping
    public ResponseEntity<WatchParty> createParty(@RequestBody WatchParty request) {
        try {
            WatchParty party = watchPartyService.createParty(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(party);
        } catch (IllegalArgumentException e) {
            log.error("Invalid party creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating party", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{partyId}/join")
    public ResponseEntity<Void> joinParty(
            @PathVariable String partyId,
            @RequestBody UserDataDTO user) {
        try {
            watchPartyService.joinParty(partyId, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid join party request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error joining party", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{partyId}/leave")
    public ResponseEntity<Void> leaveParty(
            @PathVariable String partyId,
            @RequestParam Long userId) {
        try {
            watchPartyService.leaveParty(partyId, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid leave party request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error leaving party", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{partyId}")
    public ResponseEntity<WatchPartyResponse> getParty(@PathVariable String partyId) {
        try {
            WatchPartyResponse party = watchPartyService.getPartyWithMembers(partyId);
            if (party == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(party);
        } catch (Exception e) {
            log.error("Error getting party", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{partyId}")
    public ResponseEntity<Void> deleteParty(@PathVariable String partyId) {
        try {
            watchPartyService.deleteParty(partyId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting party", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}