package org.example.backend.hateSpeach;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;


@Service
@RequiredArgsConstructor
public class HateSpeachService {
    @Value("${hatespeech.model.url}")
    private String url;
    private final RestTemplate restTemplate;

    public boolean analyzeText(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Safe JSON string
        String body = "{\"text\":\"" + text.replace("\"", "\\\"") + "\"}";

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<Boolean> response = restTemplate.postForEntity(url, request, Boolean.class);
        return response.getBody();
    }

}
