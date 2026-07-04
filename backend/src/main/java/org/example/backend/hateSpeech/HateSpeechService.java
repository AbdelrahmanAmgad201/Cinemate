package org.example.backend.hateSpeech;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HateSpeechService {
    @Value("${hatespeech.model.url}")
    private String url;

    @Value("${hatespeech.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    /**
     * Analyzes text for hate speech.
     *
     * @return true if the text is safe, false if hate speech is detected.
     *         Fails open (returns true) if the moderation service is unavailable,
     *         to avoid blocking content creation when hate-api is down.
     */
    public boolean analyzeText(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-API-Key", apiKey);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("text", text), headers);
            ResponseEntity<Boolean> response = restTemplate.postForEntity(url, request, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (RestClientException e) {
            log.warn("Hate-speech service unavailable ({}), failing open — content will be allowed: {}",
                    url, e.getMessage());
            return true; // fail-open: allow content when moderation is unreachable
        }
    }

}

