package org.example.watchparty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.watchparty.dtos.PartyEvent;
import org.example.watchparty.dtos.PartyEventType;
import org.example.watchparty.redis.RedisPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyEventService {

    private final RedisPublisher redisPublisher;

    /**
     * Publishes a user joined notification to all party members
     */
    public void notifyUserJoined(String partyId, Long userId, String userName) {
        PartyEvent event = PartyEvent.builder()
                .partyId(partyId)
                .userId(userId)
                .userName(userName)
                .eventType(PartyEventType.USER_JOINED)
                .timestamp(LocalDateTime.now())
                .payload(String.format("%s joined the party", userName))
                .build();

        redisPublisher.publish(partyId, event);
        log.info("Published USER_JOINED event for user {} in party {}", userName, partyId);
    }

    /**
     * Publishes a user left notification to all party members
     */
    public void notifyUserLeft(String partyId, Long userId, String userName) {
        PartyEvent event = PartyEvent.builder()
                .partyId(partyId)
                .userId(userId)
                .userName(userName)
                .eventType(PartyEventType.USER_LEFT)
                .timestamp(LocalDateTime.now())
                .payload(String.format("%s left the party", userName))
                .build();

        redisPublisher.publish(partyId, event);
        log.info("Published USER_LEFT event for user {} in party {}", userName, partyId);
    }

    /**
     * Publishes a party deleted notification to all members
     */
    public void notifyPartyDeleted(String partyId, String reason) {
        PartyEvent event = PartyEvent.builder()
                .partyId(partyId)
                .eventType(PartyEventType.PARTY_DELETED)
                .timestamp(LocalDateTime.now())
                .payload(reason != null ? reason : "Party has been deleted")
                .build();

        redisPublisher.publish(partyId, event);
        log.info("Published PARTY_DELETED event for party {}", partyId);
    }
}