package org.example.watchparty.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyEvent {
    private String partyId;
    private Long userId;
    private String userName;
    private PartyEventType eventType;
    private Object payload;
    private LocalDateTime timestamp;
}