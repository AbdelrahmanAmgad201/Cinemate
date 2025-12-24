package org.example.watchparty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// the entity itself stored in redis
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchParty {
    private String partyId;
    private Long movieId;
    private String movieUrl;
    private Long hostId;
    private String hostName;
    private Integer currentParticipants;
    private String status;
    private LocalDateTime createdAt;
}
