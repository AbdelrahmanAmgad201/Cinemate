package org.example.backend.watchparty;

import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.example.backend.watchparty.DTOs.WatchPartyDetailsResponse;
import org.example.backend.watchparty.DTOs.WatchPartyUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchPartyServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private WatchPartyRepository watchPartyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WatchPartyService watchPartyService;

    private Movie testMovie;
    private User testUser;
    private WatchParty testWatchParty;
    private WatchPartyUserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(watchPartyService, "watchPartyServiceUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(watchPartyService, "apiKey", "test-api-key");

        testMovie = new Movie();
        testMovie.setMovieID(1L);
        testMovie.setMovieUrl("http://example.com/movie.mp4");

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("testuser");

        testUserDTO = WatchPartyUserDTO.builder()
                .userId(1L)
                .userName("testuser")
                .build();

        testWatchParty = WatchParty.builder()
                .id(1L)
                .partyId("test-party-id")
                .movie(testMovie)
                .user(testUser)
                .status(WatchPartyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_ShouldCreateWatchParty_WhenValidInput() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(watchPartyRepository.save(any(WatchParty.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(Map.of(), HttpStatus.OK));

        WatchParty result = watchPartyService.create(testUserDTO, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getPartyId()).isNotNull();
        assertThat(result.getMovie()).isEqualTo(testMovie);
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getStatus()).isEqualTo(WatchPartyStatus.ACTIVE);

        verify(movieRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(watchPartyRepository).save(any(WatchParty.class));
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void create_ShouldThrowException_WhenMovieNotFound() {
        when(movieRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchPartyService.create(testUserDTO, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Movie not found");

        verify(movieRepository).findById(1L);
        verify(userRepository, never()).findById(anyLong());
        verify(watchPartyRepository, never()).save(any());
    }

    @Test
    void create_ShouldThrowException_WhenUserNotFound() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchPartyService.create(testUserDTO, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(movieRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(watchPartyRepository, never()).save(any());
    }

    @Test
    void create_ShouldThrowException_WhenMicroserviceFails() {
        // Arrange
        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> watchPartyService.create(testUserDTO, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to initialize watch party");

        verify(watchPartyRepository, never()).save(any());
    }

    @Test
    void get_ShouldReturnWatchPartyDetails_WhenPartyExists() {
        // Arrange
        WatchPartyDetailsResponse mockResponse = WatchPartyDetailsResponse.builder()
                .partyId("test-party-id")
                .movieId(1L)
                .movieUrl("http://example.com/movie.mp4")
                .hostId(1L)
                .hostName("testuser")
                .currentParticipants(1)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .members(Set.of(testUserDTO))
                .build();

        when(watchPartyRepository.findByPartyId("test-party-id")).thenReturn(Optional.of(testWatchParty));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(WatchPartyDetailsResponse.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Act
        WatchPartyDetailsResponse result = watchPartyService.get("test-party-id");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPartyId()).isEqualTo("test-party-id");
        assertThat(result.getMovieId()).isEqualTo(1L);
        assertThat(result.getHostId()).isEqualTo(1L);

        verify(watchPartyRepository).findByPartyId("test-party-id");
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(WatchPartyDetailsResponse.class));
    }

    @Test
    void get_ShouldThrowException_WhenPartyNotFound() {
        // Arrange
        when(watchPartyRepository.findByPartyId("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchPartyService.get("non-existent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Watch party not found");

        verify(watchPartyRepository).findByPartyId("non-existent");
        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(Class.class));
    }

    @Test
    void get_ShouldThrowException_WhenPartyIsNotActive() {
        // Arrange
        testWatchParty.setStatus(WatchPartyStatus.ENDED);
        when(watchPartyRepository.findByPartyId("test-party-id")).thenReturn(Optional.of(testWatchParty));

        assertThatThrownBy(() -> watchPartyService.get("test-party-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Watch party is no longer active");

        verify(watchPartyRepository).findByPartyId("test-party-id");
        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(Class.class));
    }

    @Test
    void join_ShouldJoinWatchParty_WhenValidInput() {
        // Arrange
        WatchPartyDetailsResponse mockResponse = WatchPartyDetailsResponse.builder()
                .partyId("test-party-id")
                .movieId(1L)
                .currentParticipants(2)
                .build();

        when(watchPartyRepository.findByPartyId("test-party-id")).thenReturn(Optional.of(testWatchParty));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(restTemplate.exchange(
                contains("/join"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        when(restTemplate.exchange(
                endsWith("/test-party-id"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(WatchPartyDetailsResponse.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Act
        WatchPartyDetailsResponse result = watchPartyService.join(testUserDTO, "test-party-id");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPartyId()).isEqualTo("test-party-id");

        verify(watchPartyRepository, times(2)).findByPartyId("test-party-id");
        verify(userRepository).findById(1L);
    }

    @Test
    void join_ShouldThrowException_WhenPartyNotActive() {
        // Arrange
        testWatchParty.setStatus(WatchPartyStatus.ENDED);
        when(watchPartyRepository.findByPartyId("test-party-id")).thenReturn(Optional.of(testWatchParty));

        assertThatThrownBy(() -> watchPartyService.join(testUserDTO, "test-party-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Watch party is no longer active");

        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void join_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(watchPartyRepository.findByPartyId("test-party-id")).thenReturn(Optional.of(testWatchParty));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchPartyService.join(testUserDTO, "test-party-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(watchPartyRepository).findByPartyId("test-party-id");
        verify(userRepository).findById(1L);
    }

    @Test
    void leave_ShouldLeaveWatchParty_WhenUserIsNotHost() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(2L);

        when(watchPartyRepository.findByPartyId("test-party-id")).thenReturn(Optional.of(testWatchParty));
        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
        when(restTemplate.exchange(
                contains("/leave"),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act
        watchPartyService.leave(2L, "test-party-id");

        // Assert
        verify(watchPartyRepository).findByPartyId("test-party-id");
        verify(userRepository).findById(2L);
        verify(restTemplate).exchange(contains("/leave"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void delete_ShouldDeleteWatchParty_WhenUserIsHost() {
        // Arrange
        when(watchPartyRepository.findByPartyId("test-party-id")).thenReturn(Optional.of(testWatchParty));
        when(restTemplate.exchange(
                endsWith("/test-party-id"),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        when(watchPartyRepository.save(any(WatchParty.class))).thenReturn(testWatchParty);

        // Act
        watchPartyService.delete(1L, "test-party-id");

        // Assert
        ArgumentCaptor<WatchParty> captor = ArgumentCaptor.forClass(WatchParty.class);
        verify(watchPartyRepository).save(captor.capture());

        WatchParty savedParty = captor.getValue();
        assertThat(savedParty.getStatus()).isEqualTo(WatchPartyStatus.ENDED);
        assertThat(savedParty.getEndedAt()).isNotNull();

        verify(restTemplate).exchange(endsWith("/test-party-id"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void delete_ShouldThrowException_WhenUserIsNotHost() {
        when(watchPartyRepository.findByPartyId("test-party-id")).thenReturn(Optional.of(testWatchParty));

        assertThatThrownBy(() -> watchPartyService.delete(2L, "test-party-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only the host can delete the watch party");

        verify(watchPartyRepository).findByPartyId("test-party-id");
        verify(watchPartyRepository, never()).save(any());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(Class.class));
    }

    @Test
    void delete_ShouldThrowException_WhenPartyNotFound() {
        when(watchPartyRepository.findByPartyId("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchPartyService.delete(1L, "non-existent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Watch party not found");

        verify(watchPartyRepository).findByPartyId("non-existent");
        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(Class.class));
    }
}