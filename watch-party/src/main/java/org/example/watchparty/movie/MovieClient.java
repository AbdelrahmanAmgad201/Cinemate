package org.example.watchparty.movie;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.watchparty.dtos.MovieMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reads movie metadata from the backend's catalog — the one piece of data the watch-party
 * domain doesn't own. A read-only, idempotent call to the backend's public movie endpoint;
 * it carries no consistency coupling (unlike the old synchronous dual-write), so a failure
 * simply fails the party-create instead of leaving two stores disagreeing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovieClient {

    private final RestTemplate restTemplate;

    @Value("${backend.url}")
    private String backendUrl;

    /**
     * Fetches the movie or fails the request with a client-appropriate status:
     * 404 if the movie doesn't exist or has no playable URL, 502 if the backend is
     * unreachable.
     */
    public MovieMetadata getMovie(Long movieId) {
        String url = backendUrl + "/api/movie/v1/get-specific-movie/" + movieId;
        try {
            MovieMetadata movie = restTemplate.postForObject(url, null, MovieMetadata.class);
            if (movie == null || movie.getMovieUrl() == null || movie.getMovieUrl().isBlank()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Movie " + movieId + " not found or has no playable URL");
            }
            return movie;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found: " + movieId);
        } catch (RestClientException e) {
            log.error("Movie lookup failed for movieId={}", movieId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to reach the movie catalog", e);
        }
    }
}
