package org.example.watchparty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.watchparty.redis.RedisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/watch-party")
@RequiredArgsConstructor
public class WatchPartyController {

    private final WatchPartyService watchPartyService;
    private final RedisService redisService;


    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initialize(@RequestBody WatchParty request) {
        try {
            log.info("Creating watch party for movie: {} by host: {}",
                    request.getMovieId(), request.getHostId());

            WatchParty createdParty = watchPartyService.create(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Watch party created successfully");
            response.put("party", createdParty);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating watch party", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to create watch party");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/redis/test")
    public ResponseEntity<Map<String, Object>> testRedis() {
        try {
            String testKey = "test:connection";
            String testValue = "Hello from Redis at " + System.currentTimeMillis();

            redisService.setValue(testKey, testValue);
            Object retrieved = redisService.getValue(testKey);

            Map<String, Object> response = new HashMap<>();
            response.put("stored", testValue);
            response.put("retrieved", retrieved);
            response.put("match", testValue.equals(retrieved));
            response.put("redis_connected", testValue.equals(retrieved));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Redis connection test failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}