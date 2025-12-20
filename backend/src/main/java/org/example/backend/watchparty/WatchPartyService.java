package org.example.backend.watchparty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.example.backend.watchparty.DTOs.WatchPartyCreationDTO;
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

    private void callMicroserviceInitialize(
            String partyId,
            Long movieId,
            String movieUrl,
            WatchPartyUserDTO host
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        WatchPartyCreationDTO request = WatchPartyCreationDTO.builder()
                .partyId(partyId)
                .movieId(movieId)
                .movieUrl(movieUrl)
                .hostId(host.getUserId())
                .hostName(host.getUserName())
                .currentParticipants(1)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        HttpEntity<WatchPartyCreationDTO> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                watchPartyServiceUrl + "/api/watch-party/initialize",
                HttpMethod.POST,
                entity,
                Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null
                || Boolean.FALSE.equals(response.getBody().get("success"))) {

            throw new IllegalStateException(
                    "Failed to initialize watch party in microservice"
            );
        }

        log.info("Watch-party microservice initialized partyId={}", partyId);
    }


}