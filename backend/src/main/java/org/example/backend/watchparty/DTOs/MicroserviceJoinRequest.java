package org.example.backend.watchparty.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicroserviceJoinRequest {
    private Long userId;
    private String userName;
}