package org.example.watchparty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.watchparty.dtos.MovieMetadata;
import org.example.watchparty.dtos.UserDataDTO;
import org.example.watchparty.dtos.WatchParty;
import org.example.watchparty.dtos.WatchPartyCreatedResponse;
import org.example.watchparty.dtos.WatchPartyResponse;
import org.example.watchparty.movie.MovieClient;
import org.example.watchparty.redis.RedisService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Owns the entire watch-party session domain (Stage 1): lifecycle, membership, host
 * authority and status all live here, backed by Redis. The one datum it doesn't own — the
 * movie's playable URL — is fetched read-only from the backend catalog via {@link MovieClient}.
 * There is no longer a backend control-plane copy to keep in sync.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchPartyService {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final PartyEventService partyEventService;
    private final MovieClient movieClient;

    private static final String PARTY_PREFIX = "party:";
    private static final String MEMBERS_SUFFIX = ":members";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final long PARTY_TTL_HOURS = 10;

    // ==================== Public API (reached via the gateway) ====================

    /**
     * Creates a new party for {@code movieId}, hosted by the authenticated caller. The
     * movie's playable URL is fetched from the backend catalog; everything else — including
     * the party id — is owned and generated here.
     */
    public WatchPartyCreatedResponse create(Long hostId, String hostName, Long movieId) {
        if (movieId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movie ID is required");
        }
        MovieMetadata movie = movieClient.getMovie(movieId); // validates existence + playable URL

        String partyId = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();

        WatchParty party = WatchParty.builder()
                .partyId(partyId)
                .movieId(movieId)
                .movieUrl(movie.getMovieUrl())
                .hostId(hostId)
                .hostName(hostName)
                .currentParticipants(1)
                .status("ACTIVE")
                .createdAt(createdAt)
                .build();

        String partyKey = PARTY_PREFIX + partyId;
        savePartyToRedis(party, partyKey);

        String membersKey = partyKey + MEMBERS_SUFFIX;
        redisService.addToSet(membersKey, createMemberKey(hostId));
        redisService.expire(membersKey, PARTY_TTL_HOURS, TimeUnit.HOURS);

        storeUserData(partyKey, hostId, hostName);

        log.info("Created watch party {} for movie {} by host {} ({})",
                partyId, movieId, hostName, hostId);

        return WatchPartyCreatedResponse.builder()
                .partyId(partyId)
                .movieId(movieId)
                .status("ACTIVE")
                .createdAt(createdAt)
                .userId(hostId)
                .build();
    }

    /** Party details with members; 404 if it doesn't exist. */
    public WatchPartyResponse get(String partyId) {
        WatchPartyResponse response = getPartyWithMembers(partyId);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Watch party not found: " + partyId);
        }
        return response;
    }

    /** The authenticated caller joins {@code partyId}; returns the updated details. */
    public WatchPartyResponse join(Long userId, String userName, String partyId) {
        UserDataDTO user = new UserDataDTO();
        user.setUserId(userId);
        user.setUserName(userName);
        joinParty(partyId, user);
        return get(partyId);
    }

    /**
     * The caller leaves. If they're the host, the party ends for everyone (mirrors the
     * previous behaviour, but the authority now lives here rather than in the backend).
     */
    public void leave(Long userId, String partyId) {
        Long hostId = requirePartyHost(partyId);
        if (hostId.equals(userId)) {
            deleteParty(partyId);
            log.info("Host {} ended party {} by leaving", userId, partyId);
            return;
        }
        leaveParty(partyId, userId);
    }

    /** Host-only deletion. */
    public void delete(Long userId, String partyId) {
        Long hostId = requirePartyHost(partyId);
        if (!hostId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can delete the party");
        }
        deleteParty(partyId);
        log.info("Party {} deleted by host {}", partyId, userId);
    }

    // ==================== Domain operations ====================

    /**
     * Allows a user to join an existing party with duplicate prevention
     */
    public void joinParty(String partyId, UserDataDTO user) {
        if (!partyExists(partyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Watch party not found: " + partyId);
        }

        String partyKey = PARTY_PREFIX + partyId;
        String membersKey = partyKey + MEMBERS_SUFFIX;
        String memberKey = createMemberKey(user.getUserId());

        // Atomic check-membership + add + increment (REL-NEW-01) — see RedisService
        // for why doing these as three separate calls can overcount participants.
        boolean joined = redisService.addToSetAndIncrementHash(
                membersKey, partyKey, "currentParticipants", memberKey);
        if (!joined) {
            log.info("User {} ({}) already in party {}", user.getUserName(), user.getUserId(), partyId);
            return;
        }

        storeUserData(partyKey, user.getUserId(), user.getUserName());
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Watch party not found: " + partyId);
        }

        String partyKey = PARTY_PREFIX + partyId;
        String membersKey = partyKey + MEMBERS_SUFFIX;
        String memberKey = createMemberKey(userId);
        String userDataKey = partyKey + ":user:" + userId;

        // Read the name before removal, for the "user left" notification.
        String userName = getUserName(userDataKey);

        // Atomic check-membership + remove + decrement (REL-NEW-03), mirroring the join
        // path — see RedisService for why separate calls can drift the count vs the member
        // set and wrongly tear down (or strand) a party.
        long newCount = redisService.removeFromSetAndDecrementHash(
                membersKey, partyKey, "currentParticipants", memberKey);
        if (newCount < 0) {
            log.warn("User {} not in party {}", userId, partyId);
            return;
        }

        redisService.deleteKey(userDataKey);

        if (newCount > 0) {
            partyEventService.notifyUserLeft(partyId, userId, userName);
            log.info("User {} left party {}", userId, partyId);
        } else {
            deletePartyInternal(partyId, "No participants remaining");
            log.info("Party {} deleted - no participants remaining", partyId);
        }
    }

    /**
     * Retrieves party details with all members, or {@code null} if the party doesn't exist.
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

    // Confirms the party exists and returns its host id, for authorization decisions.
    private Long requirePartyHost(String partyId) {
        if (!partyExists(partyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Watch party not found: " + partyId);
        }
        Object hostId = redisService.getHashValue(PARTY_PREFIX + partyId, "hostId");
        if (hostId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Watch party not found: " + partyId);
        }
        return Long.parseLong(hostId.toString());
    }

    private void storeUserData(String partyKey, Long userId, String userName) {
        String userDataKey = partyKey + ":user:" + userId;
        try {
            UserDataDTO data = new UserDataDTO();
            data.setUserId(userId);
            data.setUserName(userName);
            redisService.setValue(userDataKey, objectMapper.writeValueAsString(data),
                    PARTY_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize user data for user {}", userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store user data", e);
        }
    }

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

        if (memberKeys == null || memberKeys.isEmpty()) {
            return members;
        }

        // Single MGET instead of one GET per member (PERF-01) — a 20-person party
        // previously meant 20 sequential Redis round-trips just to list who's in it.
        List<String> userDataKeys = memberKeys.stream()
                .map(memberKeyObj -> partyKey + ":user:" + extractUserIdFromMemberKey(memberKeyObj.toString()))
                .toList();
        List<Object> userDataValues = redisService.multiGet(userDataKeys);

        if (userDataValues != null) {
            for (Object userData : userDataValues) {
                if (userData != null) {
                    try {
                        UserDataDTO user = objectMapper.readValue(userData.toString(), UserDataDTO.class);
                        members.add(user);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize user data in party {}", partyId, e);
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
