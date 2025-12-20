package org.example.watchparty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String PARTY_PREFIX = "party:";
    private static final String MEMBERS_SUFFIX = ":members";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final long PARTY_TTL_HOURS = 10;

    /**
     * Creates a new watch party with validation and proper initialization
     */
    public WatchParty createParty(WatchParty request) {
        validateCreateRequest(request);

        // Generate party ID if not provided
        String partyId = StringUtils.hasText(request.getPartyId())
                ? request.getPartyId()
                : UUID.randomUUID().toString();

        // Build complete party object with defaults
        WatchParty party = WatchParty.builder()
                .partyId(partyId)
                .movieId(request.getMovieId())
                .movieUrl(request.getMovieUrl())
                .hostId(request.getHostId())
                .hostName(request.getHostName())
                .currentParticipants(1) // Host is first participant
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        // Store party data in Redis hash
        String partyKey = PARTY_PREFIX + partyId;
        savePartyToRedis(party, partyKey);

        // Initialize empty members set with TTL
        String membersKey = partyKey + MEMBERS_SUFFIX;
        redisService.addToSet(membersKey, createMemberKey(party.getHostId()));
        redisService.expire(membersKey, PARTY_TTL_HOURS, TimeUnit.HOURS);

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

        // Check if user already joined using userId-based key
        if (Boolean.TRUE.equals(redisService.isMemberOfSet(membersKey, memberKey))) {
            log.info("User {} ({}) already in party {}", user.getUserName(), user.getUserId(), partyId);
            return;
        }

        // Add user to party members set
        redisService.addToSet(membersKey, memberKey);

        // Store user details separately for retrieval
        String userDataKey = partyKey + ":user:" + user.getUserId();
        try {
            redisService.setValue(userDataKey, objectMapper.writeValueAsString(user),
                    PARTY_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize user data", e);
            throw new RuntimeException("Failed to join party", e);
        }

        // Increment participant count - get current value, increment, and set back
        incrementParticipantCount(partyKey);

        // Refresh TTL
        redisService.expire(membersKey, PARTY_TTL_HOURS, TimeUnit.HOURS);

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

        // Check if user is in the party
        if (Boolean.FALSE.equals(redisService.isMemberOfSet(membersKey, memberKey))) {
            log.warn("User {} not in party {}", userId, partyId);
            return;
        }

        // Remove user from members set
        redisService.removeFromSet(membersKey, memberKey);

        // Delete user data
        String userDataKey = partyKey + ":user:" + userId;
        redisService.deleteKey(userDataKey);

        // Decrement participant count
        Integer newCount = decrementParticipantCount(partyKey);

        // If no participants left, delete the party
        if (newCount != null && newCount <= 0) {
            deleteParty(partyId);
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
     * Retrieves all members of a party with their details
     */
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

    /**
     * Deletes a party and all associated data
     */
    public void deleteParty(String partyId) {
        String partyKey = PARTY_PREFIX + partyId;
        String membersKey = partyKey + MEMBERS_SUFFIX;

        // Get all member keys to delete user data
        Set<Object> memberKeys = redisService.getSetMembers(membersKey);
        if (memberKeys != null) {
            for (Object memberKeyObj : memberKeys) {
                Long userId = extractUserIdFromMemberKey(memberKeyObj.toString());
                redisService.deleteKey(partyKey + ":user:" + userId);
            }
        }

        // Delete members set and party hash
        redisService.deleteKey(membersKey);
        redisService.deleteKey(partyKey);

        log.info("Deleted party: {}", partyId);
    }

    // ==================== Helper Methods ====================

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