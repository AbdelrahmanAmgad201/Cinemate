package org.example.watchparty.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PartyEvent {
    private String partyId;
    private Long userId;
    private String userName;
    private PartyEventType eventType;
    private Object payload;
    private LocalDateTime timestamp;
}