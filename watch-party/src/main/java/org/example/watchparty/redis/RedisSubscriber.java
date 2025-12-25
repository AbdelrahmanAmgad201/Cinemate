package org.example.watchparty.redis;

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

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            PartyEvent event = JsonUtils.fromJson(json, PartyEvent.class);

            String partyId = event.getPartyId();

            // Broadcast event to all party members via WebSocket
            messagingTemplate.convertAndSend(
                    "/topic/party/" + partyId,
                    event
            );

            log.debug("Broadcasted event {} to party {}", event.getEventType(), partyId);
        } catch (Exception e) {
            log.error("Error processing Redis message", e);
        }
    }
}