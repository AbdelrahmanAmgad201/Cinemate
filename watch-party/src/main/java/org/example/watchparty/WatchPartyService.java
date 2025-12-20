package org.example.watchparty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.watchparty.redis.RedisService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchPartyService {

    private final RedisService redisService;
    private static final String PARTY_PREFIX = "party:";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Create a new watch party and store in Redis
     * Uses Hash structure for efficient field-level access
     */
    public WatchParty create(WatchParty request) {
        // Generate party ID if not provided
        if (request.getPartyId() == null || request.getPartyId().isEmpty()) {
            request.setPartyId(UUID.randomUUID().toString());
        }

        // Set creation time if not provided
        if (request.getCreatedAt() == null) {
            request.setCreatedAt(LocalDateTime.now());
        }

        // Set default values
        if (request.getCurrentParticipants() == null) {
            request.setCurrentParticipants(1); // Host is first participant
        }

        if (request.getStatus() == null) {
            request.setStatus("ACTIVE");
        }

        String partyKey = PARTY_PREFIX + request.getPartyId();

        redisService.setHashValue(partyKey, "partyId", request.getPartyId());
        redisService.setHashValue(partyKey, "movieId", request.getMovieId().toString());
        redisService.setHashValue(partyKey, "movieUrl", request.getMovieUrl());
        redisService.setHashValue(partyKey, "hostId", request.getHostId().toString());
        redisService.setHashValue(partyKey, "hostName", request.getHostName());
        redisService.setHashValue(partyKey, "currentParticipants", request.getCurrentParticipants().toString());
        redisService.setHashValue(partyKey, "status", request.getStatus());
        redisService.setHashValue(partyKey, "createdAt", request.getCreatedAt().format(FORMATTER));

        redisService.expire(partyKey, 10, TimeUnit.HOURS);


        log.info("Created watch party: {} for movie: {} by host: {}",
                request.getPartyId(), request.getMovieId(), request.getHostId());

        return request;
    }

    /**
     * Get watch party by ID
     */
    public WatchParty getParty(String partyId) {
        String partyKey = PARTY_PREFIX + partyId;

        if (!partyExists(partyId)) {
            log.warn("Party {} does not exist", partyId);
            return null;
        }

        Map<Object, Object> partyData = redisService.getAllHashValues(partyKey);

        return WatchParty.builder()
                .partyId(partyData.get("partyId").toString())
                .movieId(Long.parseLong(partyData.get("movieId").toString()))
                .movieUrl(partyData.get("movieUrl").toString())
                .hostId(Long.parseLong(partyData.get("hostId").toString()))
                .hostName(partyData.get("hostName").toString())
                .currentParticipants(Integer.parseInt(partyData.get("currentParticipants").toString()))
                .status(partyData.get("status").toString())
                .createdAt(LocalDateTime.parse(partyData.get("createdAt").toString(), FORMATTER))
                .build();
    }

    /**
     * Check if party exists
     */
    private boolean partyExists(String partyId) {
        String partyKey = PARTY_PREFIX + partyId;
        return Boolean.TRUE.equals(redisService.hasKey(partyKey));
    }


}