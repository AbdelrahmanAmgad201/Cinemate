package org.example.watchparty.redis;

import lombok.RequiredArgsConstructor;
import org.example.watchparty.dtos.PartyEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final StringRedisTemplate redisTemplate;

    public void publish(String partyId, PartyEvent event) {
        redisTemplate.convertAndSend(
                "party:" + partyId,
                JsonUtils.toJson(event)
        );
    }
}