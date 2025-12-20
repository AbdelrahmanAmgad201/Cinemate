package org.example.backend.watchparty.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicroserviceInitRequest {
    private String partyId;
    private Long movieId;
    private String movieUrl;
    private Long hostId;
    private String hostName;
}