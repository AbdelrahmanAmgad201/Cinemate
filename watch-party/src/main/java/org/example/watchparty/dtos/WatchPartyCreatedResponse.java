package org.example.watchparty.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response for party creation. {@code userId} is the host's id — the field name matches
 * what the SPA reads ({@code response.data.userId}) so routing the call straight to this
 * service (instead of through the backend) is transparent to the client.
 */
@Data
@Builder
public class WatchPartyCreatedResponse {
    private String partyId;
    private Long movieId;
    private String status;
    private LocalDateTime createdAt;
    private Long userId;
}
