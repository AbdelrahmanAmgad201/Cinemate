package org.example.backend.watchparty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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

    @Value("${watchparty.service.api.key:secret-key}")
    private String apiKey;

    @Transactional(readOnly = true)
    public WatchPartyDetailsResponse get(WatchPartyUserDTO userDTO, String partyId) {
        WatchParty watchParty = watchPartyRepository.findByPartyId(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        if (watchParty.getStatus() == WatchPartyStatus.ENDED) {
            throw new RuntimeException("This party has ended");
        }

        // Call microservice to get party details
        WatchPartyDetails details = callMicroserviceGetParty(partyId, userDTO.getUserId());

        WatchPartyDetailsResponse response = new WatchPartyDetailsResponse();
        response.setPartyId(details.getPartyId());
        response.setMovieId(details.getMovieId());
        response.setMovieUrl(details.getMovieUrl());
        response.setHostId(details.getHostId());
        response.setIsHost(details.getIsHost());
        response.setCurrentParticipants(details.getCurrentParticipants());
        response.setMembers(details.getMembers());
        response.setStatus(details.getStatus());
        response.setCreatedAt(details.getCreatedAt());

        return response;
    }

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

    @Transactional
    public void delete(Long userId, String partyId) {
        WatchParty watchParty = watchPartyRepository.findByPartyId(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        if (!watchParty.getUserId().equals(userId)) {
            throw new RuntimeException("Only host can delete party");
        }

        // Call microservice to delete party
        callMicroserviceDelete(partyId);

        watchParty.setStatus(WatchPartyStatus.ENDED);
        watchParty.setEndedAt(LocalDateTime.now());
        watchPartyRepository.save(watchParty);
    }

    @Transactional
    public WatchPartyDetailsResponse join(WatchPartyUserDTO userDTO, String partyId) {
        WatchParty watchParty = watchPartyRepository.findByPartyId(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        if (watchParty.getStatus() == WatchPartyStatus.ENDED) {
            throw new RuntimeException("This party has ended");
        }

        // Call microservice to join party
        callMicroserviceJoin(partyId, userDTO);

        return get(userDTO, partyId);
    }

    // ==================== MICROSERVICE CALLS ====================

    private void callMicroserviceInitialize(String partyId, Long movieId, String movieUrl, WatchPartyUserDTO host) {
        // TODO: POST {watchPartyServiceUrl}/api/parties/initialize
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);

        MicroserviceInitRequest request = MicroserviceInitRequest.builder()
                .partyId(partyId)
                .movieId(movieId)
                .movieUrl(movieUrl)
                .hostId(host.getUserId())
                .hostName(host.getUserName())
                .build();

        HttpEntity<MicroserviceInitRequest> entity = new HttpEntity<>(request, headers);

        // TODO: Uncomment when microservice is ready
        // restTemplate.exchange(
        //     watchPartyServiceUrl + "/api/parties/initialize",
        //     HttpMethod.POST,
        //     entity,
        //     Void.class
        // );

        log.info("Called microservice to initialize party: {}", partyId);
    }

    private WatchPartyDetails callMicroserviceGetParty(String partyId, Long userId) {
        // TODO: GET {watchPartyServiceUrl}/api/parties/{partyId}?userId={userId}
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // TODO: Uncomment when microservice is ready
        // ResponseEntity<MicroservicePartyResponse> response = restTemplate.exchange(
        //     watchPartyServiceUrl + "/api/parties/" + partyId + "?userId=" + userId,
        //     HttpMethod.GET,
        //     entity,
        //     MicroservicePartyResponse.class
        // );
        // return mapToWatchPartyDetails(response.getBody());

        log.info("Called microservice to get party: {}", partyId);

        // Temporary mock response
        return WatchPartyDetails.builder()
                .partyId(partyId)
                .movieId(1L)
                .movieUrl("http://example.com/movie.mp4")
                .hostId(userId)
                .isHost(true)
                .currentParticipants(1)
                .members(java.util.List.of())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void callMicroserviceJoin(String partyId, WatchPartyUserDTO user) {
        // TODO: POST {watchPartyServiceUrl}/api/parties/{partyId}/join
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);

        MicroserviceJoinRequest request = MicroserviceJoinRequest.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .build();

        HttpEntity<MicroserviceJoinRequest> entity = new HttpEntity<>(request, headers);

        // TODO: Uncomment when microservice is ready
        // restTemplate.exchange(
        //     watchPartyServiceUrl + "/api/parties/" + partyId + "/join",
        //     HttpMethod.POST,
        //     entity,
        //     Void.class
        // );

        log.info("Called microservice to join party: {} by user: {}", partyId, user.getUserId());
    }

    private void callMicroserviceDelete(String partyId) {
        // TODO: DELETE {watchPartyServiceUrl}/api/parties/{partyId}
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // TODO: Uncomment when microservice is ready
        // restTemplate.exchange(
        //     watchPartyServiceUrl + "/api/parties/" + partyId,
        //     HttpMethod.DELETE,
        //     entity,
        //     Void.class
        // );

        log.info("Called microservice to delete party: {}", partyId);
    }
}