package org.example.backend.watchparty.DTOs;

import lombok.Builder;
import lombok.Data;
import org.example.backend.watchparty.WatchParty;

import java.time.LocalDateTime;

/**
 * Response for party creation (CQ-NEW-03) — returned instead of the {@link WatchParty}
 * entity itself.
 */
@Data
@Builder
public class WatchPartyCreatedResponse {
    private String partyId;
    private Long movieId;
    private String status;
    private LocalDateTime createdAt;
    private Long userId;

    public static WatchPartyCreatedResponse from(WatchParty watchParty) {
        return WatchPartyCreatedResponse.builder()
                .partyId(watchParty.getPartyId())
                .movieId(watchParty.getMovieId())
                .status(watchParty.getStatus().name())
                .createdAt(watchParty.getCreatedAt())
                .userId(watchParty.getUserId())
                .build();
    }
}
