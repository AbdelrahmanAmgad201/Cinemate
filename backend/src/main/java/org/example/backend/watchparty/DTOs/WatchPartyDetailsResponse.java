package org.example.backend.watchparty.DTOs;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.user.UserDataDTO;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchPartyDetailsResponse {
    private String partyId;
    private Long movieId;
    private String movieUrl;
    private Long hostId;
    private String hostName;
    private Integer currentParticipants;
    private String status;
    private LocalDateTime createdAt;
    private Set<WatchPartyUserDTO> members;
}