package org.example.backend.watchparty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.example.backend.watchparty.DTOs.WatchPartyCreationDTO;
import org.example.backend.watchparty.DTOs.WatchPartyDetailsResponse;
import org.example.backend.watchparty.DTOs.WatchPartyUserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchPartyService {
    private final MovieRepository movieRepository;
    private final WatchPartyRepository watchPartyRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${watchparty.service.url:http://localhost:8081}")
    private String watchPartyServiceUrl;

    @Value("${watchparty.service.internal.api.key}")
    private String apiKey;

    /**
     * Creates a new watch party
     */
    @Transactional
    public WatchParty create(WatchPartyUserDTO userDTO, Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        User user = userRepository.findById(userDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String partyId = UUID.randomUUID().toString();

        // Call microservice to initialize party
        callMicroserviceInitialize(partyId, movieId, movie.getMovieUrl(), userDTO);

        WatchParty watchParty = WatchParty.builder()
                .partyId(partyId)
                .movie(movie)
                .user(user)
                .status(WatchPartyStatus.ACTIVE)
                .build();

        return watchPartyRepository.save(watchParty);
    }

    /**
     * Gets watch party details from microservice
     */
    @Transactional(readOnly = true)
    public WatchPartyDetailsResponse get(String partyId) {
        // Verify party exists in database
        WatchParty watchParty = watchPartyRepository.findByPartyId(partyId)
                .orElseThrow(() -> new RuntimeException("Watch party not found"));

        // Verify party is still active
        if (watchParty.getStatus() != WatchPartyStatus.ACTIVE) {
            throw new RuntimeException("Watch party is no longer active");
        }

        // Get full details from microservice
        WatchPartyDetailsResponse response = callMicroserviceGetDetails(partyId);


        return response;
    }

    /**
     * Joins an existing watch party
     */
    @Transactional
    public WatchPartyDetailsResponse join(WatchPartyUserDTO userDTO, String partyId) {
        // Verify party exists and is active
        WatchParty watchParty = watchPartyRepository.findByPartyId(partyId)
                .orElseThrow(() -> new RuntimeException("Watch party not found"));

        if (watchParty.getStatus() != WatchPartyStatus.ACTIVE) {
            throw new RuntimeException("Watch party is no longer active");
        }

        // Verify user exists
        User user = userRepository.findById(userDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Call microservice to join party
        callMicroserviceJoin(partyId, userDTO);

        return get(partyId);

    }

    /**
     * Leaves a watch party
     */
    @Transactional
    public void leave(Long userId, String partyId) {
        // Verify party exists
        WatchParty watchParty = watchPartyRepository.findByPartyId(partyId)
                .orElseThrow(() -> new RuntimeException("Watch party not found"));

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // If user is the host, delete the entire party
        if (watchParty.getUser().getId().equals(userId)) {
            delete(userId, partyId);
            return;
        }

        // Call microservice to leave party
        callMicroserviceLeave(partyId, userId);

        log.info("User {} left watch party {}", userId, partyId);
    }

    /**
     * Deletes a watch party (host only)
     */
    @Transactional
    public void delete(Long userId, String partyId) {
        WatchParty watchParty = watchPartyRepository.findByPartyId(partyId)
                .orElseThrow(() -> new RuntimeException("Watch party not found"));

        // Verify user is the host
        if (!watchParty.getUser().getId().equals(userId)) {
            throw new RuntimeException("Only the host can delete the watch party");
        }

        // Call microservice to delete party
        callMicroserviceDelete(partyId);

        // Update status in database
        watchParty.setStatus(WatchPartyStatus.ENDED);
        watchParty.setEndedAt(LocalDateTime.now());
        watchPartyRepository.save(watchParty);

        log.info("Watch party {} deleted by host {}", partyId, userId);
    }

    // ==================== Microservice Calls ====================

    private void callMicroserviceInitialize(
            String partyId,
            Long movieId,
            String movieUrl,
            WatchPartyUserDTO host
    ) {
        HttpHeaders headers = createHeaders();

        WatchPartyCreationDTO request = WatchPartyCreationDTO.builder()
                .partyId(partyId)
                .movieId(movieId)
                .movieUrl(movieUrl)
                .hostId(host.getUserId())
                .hostName(host.getUserName())
                .build();

        HttpEntity<WatchPartyCreationDTO> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    watchPartyServiceUrl + "/api/watch-party",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Failed to initialize watch party in microservice");
            }

            log.info("Watch-party microservice initialized partyId={}", partyId);
        } catch (Exception e) {
            log.error("Error calling microservice to initialize party", e);
            throw new RuntimeException("Failed to initialize watch party", e);
        }
    }

    private WatchPartyDetailsResponse callMicroserviceGetDetails(String partyId) {
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<WatchPartyDetailsResponse> response = restTemplate.exchange(
                    watchPartyServiceUrl + "/api/watch-party/" + partyId,
                    HttpMethod.GET,
                    entity,
                    WatchPartyDetailsResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Failed to get watch party details from microservice");
            }
            System.out.println(response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling microservice to get party details", e);
            throw new RuntimeException("Failed to get watch party details", e);
        }
    }

    private void callMicroserviceJoin(String partyId, WatchPartyUserDTO userDTO) {
        HttpHeaders headers = createHeaders();
        HttpEntity<WatchPartyUserDTO> entity = new HttpEntity<>(userDTO, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    watchPartyServiceUrl + "/api/watch-party/" + partyId + "/join",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Failed to join watch party in microservice");
            }

            log.info("User {} joined watch party {} in microservice", userDTO.getUserId(), partyId);
        } catch (Exception e) {
            log.error("Error calling microservice to join party", e);
            throw new RuntimeException("Failed to join watch party", e);
        }
    }

    private void callMicroserviceLeave(String partyId, Long userId) {
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    watchPartyServiceUrl + "/api/watch-party/" + partyId + "/leave?userId=" + userId,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Failed to leave watch party in microservice");
            }

            log.info("User {} left watch party {} in microservice", userId, partyId);
        } catch (Exception e) {
            log.error("Error calling microservice to leave party", e);
            throw new RuntimeException("Failed to leave watch party", e);
        }
    }

    private void callMicroserviceDelete(String partyId) {
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    watchPartyServiceUrl + "/api/watch-party/" + partyId,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Failed to delete watch party in microservice");
            }

            log.info("Watch party {} deleted in microservice", partyId);
        } catch (Exception e) {
            log.error("Error calling microservice to delete party", e);
            throw new RuntimeException("Failed to delete watch party", e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("X-Internal-API-Key", apiKey);
        return headers;
    }
}