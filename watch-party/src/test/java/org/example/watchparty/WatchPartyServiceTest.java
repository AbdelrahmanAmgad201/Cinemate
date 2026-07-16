package org.example.watchparty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.watchparty.dtos.MovieMetadata;
import org.example.watchparty.dtos.UserDataDTO;
import org.example.watchparty.dtos.WatchPartyCreatedResponse;
import org.example.watchparty.dtos.WatchPartyResponse;
import org.example.watchparty.movie.MovieClient;
import org.example.watchparty.redis.RedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchPartyServiceTest {

    @Mock
    private RedisService redisService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PartyEventService partyEventService;

    @Mock
    private MovieClient movieClient;

    @InjectMocks
    private WatchPartyService watchPartyService;

    @Test
    void create_ValidInputs_ReturnsCreatedResponseWithPartyId() throws JsonProcessingException {
        MovieMetadata movie = new MovieMetadata();
        movie.setMovieUrl("http://movie.url");
        when(movieClient.getMovie(100L)).thenReturn(movie);
        when(objectMapper.writeValueAsString(any(UserDataDTO.class))).thenReturn("{\"userId\":1,\"userName\":\"Host\"}");

        WatchPartyCreatedResponse response = watchPartyService.create(1L, "Host", 100L);

        assertNotNull(response.getPartyId());
        assertEquals(100L, response.getMovieId());
        assertEquals("ACTIVE", response.getStatus());

        verify(redisService).setHashValue(startsWith("party:"), eq("movieId"), eq("100"));
        verify(redisService).addToSet(startsWith("party:"), eq("user:1"));
    }

    @Test
    void create_NullMovieId_ThrowsResponseStatusException() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                watchPartyService.create(1L, "Host", null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void get_NonExistentParty_ThrowsResponseStatusException() {
        when(redisService.hasKey("party:test-party")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                watchPartyService.get("test-party"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void join_UserAlreadyInParty_IsIdempotent() {
        when(redisService.hasKey("party:test-party")).thenReturn(true);
        when(redisService.addToSetAndIncrementHash("party:test-party:members", "party:test-party", "currentParticipants", "user:2")).thenReturn(false);

        Map<Object, Object> partyData = new HashMap<>();
        partyData.put("partyId", "test-party");
        partyData.put("movieId", "100");
        partyData.put("movieUrl", "http://movie.url");
        partyData.put("hostId", "1");
        partyData.put("hostName", "Host");
        partyData.put("currentParticipants", "1");
        partyData.put("status", "ACTIVE");
        partyData.put("createdAt", LocalDateTime.now().toString());
        when(redisService.getAllHashValues("party:test-party")).thenReturn(partyData);

        WatchPartyResponse response = watchPartyService.join(2L, "User 2", "test-party");

        assertNotNull(response);
        verify(redisService, never()).addToSet(anyString(), eq("user:2")); // Not added again
    }

    @Test
    void delete_ByNonHost_ThrowsResponseStatusException() {
        when(redisService.hasKey("party:test-party")).thenReturn(true);
        when(redisService.getHashValue("party:test-party", "hostId")).thenReturn("1");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                watchPartyService.delete(2L, "test-party")); // Caller is 2

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void delete_ByHost_CallsNotifyPartyDeletedAndCleansUpRedis() {
        when(redisService.hasKey("party:test-party")).thenReturn(true);
        when(redisService.getHashValue("party:test-party", "hostId")).thenReturn("1");

        watchPartyService.delete(1L, "test-party");

        verify(partyEventService).notifyPartyDeleted(eq("test-party"), anyString());
        verify(redisService).deleteKey("party:test-party");
        verify(redisService).deleteKey("party:test-party:members");
    }

    @Test
    void leave_LastMember_DeletesParty() {
        when(redisService.hasKey("party:test-party")).thenReturn(true);
        when(redisService.getHashValue("party:test-party", "hostId")).thenReturn("1");
        
        // Caller is a non-host who happens to be the last member
        when(redisService.removeFromSetAndDecrementHash("party:test-party:members", "party:test-party", "currentParticipants", "user:2")).thenReturn(0L);

        watchPartyService.leave(2L, "test-party");

        verify(partyEventService).notifyPartyDeleted(eq("test-party"), anyString());
        verify(redisService).deleteKey("party:test-party");
    }
}
