package org.example.backend.watchparty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicroservicePartyResponse {
    private String partyId;
    private Long movieId;
    private String movieUrl;
    private Long hostId;
    private Boolean isHost;
    private Integer currentParticipants;
    private List<WatchPartyUserDTO> members;
    private String status;
    private LocalDateTime createdAt;
}