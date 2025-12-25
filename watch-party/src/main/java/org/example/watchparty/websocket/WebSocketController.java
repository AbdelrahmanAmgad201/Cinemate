package org.example.watchparty.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.watchparty.dtos.PartyEvent;
import org.example.watchparty.dtos.PartyEventType;
import org.example.watchparty.redis.RedisPublisher;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final RedisPublisher redisPublisher;

    @MessageMapping("/party/{partyId}/control")
    public void handleControl(
            @DestinationVariable String partyId,
            @Payload PartyEvent event
    ) {
        // Set timestamp if not present
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }

        // Ensure partyId is set
        event.setPartyId(partyId);

        log.info("Received {} event from user {} in party {}",
                event.getEventType(), event.getUserId(), partyId);

        // Publish to Redis for broadcasting to all party members
        redisPublisher.publish(partyId, event);
    }

    @MessageMapping("/party/{partyId}/chat")
    public void handleChat(
            @DestinationVariable String partyId,
            @Payload PartyEvent event
    ) {
        event.setEventType(PartyEventType.CHAT);
        event.setPartyId(partyId);
        event.setTimestamp(LocalDateTime.now());

        log.info("Chat message from {} in party {}", event.getUserName(), partyId);

        redisPublisher.publish(partyId, event);
    }
}