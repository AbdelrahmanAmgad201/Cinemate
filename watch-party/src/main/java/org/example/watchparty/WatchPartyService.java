package org.example.watchparty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.watchparty.dtos.UserDataDTO;
import org.example.watchparty.dtos.WatchParty;
import org.example.watchparty.dtos.WatchPartyResponse;
import org.example.watchparty.redis.RedisService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchPartyService {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final PartyEventService partyEventService;

    private static final String PARTY_PREFIX = "party:";
    private static final String MEMBERS_SUFFIX = ":members";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final long PARTY_TTL_HOURS = 10;

    /**
     * Creates a new watch party with validation and proper initialization
     */
    public WatchParty createParty(WatchParty request) {
        validateCreateRequest(request);

        String partyId = StringUtils.hasText(request.getPartyId())
                ? request.getPartyId()
                : UUID.randomUUID().toString();

        WatchParty party = WatchParty.builder()
                .partyId(partyId)
                .movieId(request.getMovieId())
                .movieUrl(request.getMovieUrl())
                .hostId(request.getHostId())
                .hostName(request.getHostName())
                .currentParticipants(1)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        String partyKey = PARTY_PREFIX + partyId;
        savePartyToRedis(party, partyKey);

        String membersKey = partyKey + MEMBERS_SUFFIX;
        redisService.addToSet(membersKey, createMemberKey(party.getHostId()));
        redisService.expire(membersKey, PARTY_TTL_HOURS, TimeUnit.HOURS);

        // Store host user data
        String hostDataKey = partyKey + ":user:" + party.getHostId();
        try {
            UserDataDTO hostData = new UserDataDTO();
            hostData.setUserId(party.getHostId());
            hostData.setUserName(party.getHostName());
            redisService.setValue(hostDataKey, objectMapper.writeValueAsString(hostData),
                    PARTY_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize host data", e);
        }

        log.info("Created watch party: {} for movie: {} by host: {} ({})",
                partyId, party.getMovieId(), party.getHostName(), party.getHostId());

        return party;
    }

    /**
     * Allows a user to join an existing party with duplicate prevention
     */
    public void joinParty(String partyId, UserDataDTO user) {
        validateJoinRequest(partyId, user);

        if (!partyExists(partyId)) {
            throw new IllegalArgumentException("Party does not exist: " + partyId);
        }

        String partyKey = PARTY_PREFIX + partyId;
        String membersKey = partyKey + MEMBERS_SUFFIX;
        String memberKey = createMemberKey(user.getUserId());

        if (Boolean.TRUE.equals(redisService.isMemberOfSet(membersKey, memberKey))) {
            log.info("User {} ({}) already in party {}", user.getUserName(), user.getUserId(), partyId);
            return;
        }

        redisService.addToSet(membersKey, memberKey);

        String userDataKey = partyKey + ":user:" + user.getUserId();
        try {
            redisService.setValue(userDataKey, objectMapper.writeValueAsString(user),
                    PARTY_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize user data", e);
            throw new RuntimeException("Failed to join party", e);
        }

        incrementParticipantCount(partyKey);
        redisService.expire(membersKey, PARTY_TTL_HOURS, TimeUnit.HOURS);

        // Notify all members about the new user
        partyEventService.notifyUserJoined(partyId, user.getUserId(), user.getUserName());

        log.info("User {} ({}) joined party {}", user.getUserName(), user.getUserId(), partyId);
    }

    /**
     * Allows a user to leave a party
     */
    public void leaveParty(String partyId, Long userId) {
        if (!partyExists(partyId)) {
            throw new IllegalArgumentException("Party does not exist: " + partyId);
        }

        String partyKey = PARTY_PREFIX + partyId;
        String membersKey = partyKey + MEMBERS_SUFFIX;
        String memberKey = createMemberKey(userId);

        if (Boolean.FALSE.equals(redisService.isMemberOfSet(membersKey, memberKey))) {
            log.warn("User {} not in party {}", userId, partyId);
            return;
        }

        // Get user name before removal for notification
        String userDataKey = partyKey + ":user:" + userId;
        String userName = getUserName(userDataKey);

        redisService.removeFromSet(membersKey, memberKey);
        redisService.deleteKey(userDataKey);

        Integer newCount = decrementParticipantCount(partyKey);

        // Notify other members that user left
        if (newCount != null && newCount > 0) {
            partyEventService.notifyUserLeft(partyId, userId, userName);
        }

        if (newCount != null && newCount <= 0) {
            deletePartyInternal(partyId, "No participants remaining");
            log.info("Party {} deleted - no participants remaining", partyId);
        } else {
            log.info("User {} left party {}", userId, partyId);
        }
    }

    /**
     * Retrieves party details with all members
     */
    public WatchPartyResponse getPartyWithMembers(String partyId) {
        if (!partyExists(partyId)) {
            log.warn("Party {} does not exist", partyId);
            return null;
        }

        String partyKey = PARTY_PREFIX + partyId;
        Map<Object, Object> partyData = redisService.getAllHashValues(partyKey);

        if (partyData == null || partyData.isEmpty()) {
            return null;
        }

        WatchParty party = buildWatchPartyFromRedis(partyData);
        Set<UserDataDTO> members = getPartyMembers(partyId);

        return WatchPartyResponse.builder()
                .partyId(party.getPartyId())
                .movieId(party.getMovieId())
                .movieUrl(party.getMovieUrl())
                .hostId(party.getHostId())
                .hostName(party.getHostName())
                .currentParticipants(party.getCurrentParticipants())
                .status(party.getStatus())
                .createdAt(party.getCreatedAt())
                .members(members)
                .build();
    }

    /**
     * Deletes a party and notifies all members
     */
    public void deleteParty(String partyId) {
        deletePartyInternal(partyId, "Party has been deleted by the host");
    }

    // ==================== Helper Methods ====================

    private void deletePartyInternal(String partyId, String reason) {
        // Notify all members before deleting
        partyEventService.notifyPartyDeleted(partyId, reason);

        String partyKey = PARTY_PREFIX + partyId;
        String membersKey = partyKey + MEMBERS_SUFFIX;

        Set<Object> memberKeys = redisService.getSetMembers(membersKey);
        if (memberKeys != null) {
            for (Object memberKeyObj : memberKeys) {
                Long userId = extractUserIdFromMemberKey(memberKeyObj.toString());
                redisService.deleteKey(partyKey + ":user:" + userId);
            }
        }

        redisService.deleteKey(membersKey);
        redisService.deleteKey(partyKey);

        log.info("Deleted party: {} - Reason: {}", partyId, reason);
    }

    private Set<UserDataDTO> getPartyMembers(String partyId) {
        String partyKey = PARTY_PREFIX + partyId;
        String membersKey = partyKey + MEMBERS_SUFFIX;

        Set<Object> memberKeys = redisService.getSetMembers(membersKey);
        Set<UserDataDTO> members = new HashSet<>();

        if (memberKeys != null) {
            for (Object memberKeyObj : memberKeys) {
                Long userId = extractUserIdFromMemberKey(memberKeyObj.toString());
                String userDataKey = partyKey + ":user:" + userId;
                Object userData = redisService.getValue(userDataKey);

                if (userData != null) {
                    try {
                        UserDataDTO user = objectMapper.readValue(userData.toString(), UserDataDTO.class);
                        members.add(user);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize user data for userId: {}", userId, e);
                    }
                }
            }
        }

        return members;
    }

    private String getUserName(String userDataKey) {
        Object userData = redisService.getValue(userDataKey);
        if (userData != null) {
            try {
                UserDataDTO user = objectMapper.readValue(userData.toString(), UserDataDTO.class);
                return user.getUserName();
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize user data", e);
            }
        }
        return "Unknown User";
    }

    private void incrementParticipantCount(String partyKey) {
        Object currentValue = redisService.getHashValue(partyKey, "currentParticipants");
        int current = Integer.parseInt(currentValue.toString());
        redisService.setHashValue(partyKey, "currentParticipants", String.valueOf(current + 1));
    }

    private Integer decrementParticipantCount(String partyKey) {
        Object currentValue = redisService.getHashValue(partyKey, "currentParticipants");
        int current = Integer.parseInt(currentValue.toString());
        int newValue = current - 1;
        redisService.setHashValue(partyKey, "currentParticipants", String.valueOf(newValue));
        return newValue;
    }

    private void validateCreateRequest(WatchParty request) {
        if (request == null) {
            throw new IllegalArgumentException("Party request cannot be null");
        }
        if (request.getMovieId() == null) {
            throw new IllegalArgumentException("Movie ID is required");
        }
        if (!StringUtils.hasText(request.getMovieUrl())) {
            throw new IllegalArgumentException("Movie URL is required");
        }
        if (request.getHostId() == null) {
            throw new IllegalArgumentException("Host ID is required");
        }
        if (!StringUtils.hasText(request.getHostName())) {
            throw new IllegalArgumentException("Host name is required");
        }
    }

    private void validateJoinRequest(String partyId, UserDataDTO user) {
        if (!StringUtils.hasText(partyId)) {
            throw new IllegalArgumentException("Party ID is required");
        }
        if (user == null) {
            throw new IllegalArgumentException("User data cannot be null");
        }
        if (user.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (!StringUtils.hasText(user.getUserName())) {
            throw new IllegalArgumentException("User name is required");
        }
    }

    private boolean partyExists(String partyId) {
        String partyKey = PARTY_PREFIX + partyId;
        return Boolean.TRUE.equals(redisService.hasKey(partyKey));
    }

    private void savePartyToRedis(WatchParty party, String partyKey) {
        redisService.setHashValue(partyKey, "partyId", party.getPartyId());
        redisService.setHashValue(partyKey, "movieId", party.getMovieId().toString());
        redisService.setHashValue(partyKey, "movieUrl", party.getMovieUrl());
        redisService.setHashValue(partyKey, "hostId", party.getHostId().toString());
        redisService.setHashValue(partyKey, "hostName", party.getHostName());
        redisService.setHashValue(partyKey, "currentParticipants", party.getCurrentParticipants().toString());
        redisService.setHashValue(partyKey, "status", party.getStatus());
        redisService.setHashValue(partyKey, "createdAt", party.getCreatedAt().format(FORMATTER));
        redisService.expire(partyKey, PARTY_TTL_HOURS, TimeUnit.HOURS);
    }

    private WatchParty buildWatchPartyFromRedis(Map<Object, Object> data) {
        return WatchParty.builder()
                .partyId(data.get("partyId").toString())
                .movieId(Long.parseLong(data.get("movieId").toString()))
                .movieUrl(data.get("movieUrl").toString())
                .hostId(Long.parseLong(data.get("hostId").toString()))
                .hostName(data.get("hostName").toString())
                .currentParticipants(Integer.parseInt(data.get("currentParticipants").toString()))
                .status(data.get("status").toString())
                .createdAt(LocalDateTime.parse(data.get("createdAt").toString(), FORMATTER))
                .build();
    }

    private String createMemberKey(Long userId) {
        return "user:" + userId;
    }

    private Long extractUserIdFromMemberKey(String memberKey) {
        return Long.parseLong(memberKey.replace("user:", ""));
    }
}