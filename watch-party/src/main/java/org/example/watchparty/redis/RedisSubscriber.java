package org.example.watchparty.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.watchparty.dtos.PartyEvent;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            PartyEvent event = objectMapper.readValue(json, PartyEvent.class);

            String partyId = event.getPartyId();

            // Broadcast event to all party members via WebSocket
            messagingTemplate.convertAndSend(
                    "/topic/watch-party/" + partyId,
                    event
            );

            log.debug("Broadcasted event {} to party {}", event.getEventType(), partyId);
        } catch (Exception e) {
            log.error("Error processing Redis message", e);
        }
    }
}