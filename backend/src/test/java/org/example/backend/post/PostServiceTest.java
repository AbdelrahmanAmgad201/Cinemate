package org.example.backend.post;

import org.example.backend.AbstractMongoIntegrationTest;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class PostServiceTest extends AbstractMongoIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @MockBean
    private RestTemplate restTemplate; // mock external API

    private final String url = "http://localhost:8000/api/hate/v1/analyze";

    // ==============================================================
    // SUCCESS CASE: AI returns true → post must be saved to DB
    // ==============================================================

    @Test
    void testAddPost_whenCleanText_shouldSavePost() {
        // Given input DTO
        AddPostDto dto = new AddPostDto(
                new ObjectId("00000000000000000000006f"),
                "Test Title",
                "Normal content"
        );
        Long userId = 5L;

        // Mock AI response: "text is clean"
        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(true, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        // When
        postService.addPost(dto, userId);

        // Then DB contains exactly 1 saved post
        Post saved = postRepository.findAll().stream().findFirst().orElse(null);

        assertThat(saved).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Test Title");
        assertThat(saved.getContent()).isEqualTo("Normal content");
        assertThat(saved.getForumId()).isEqualTo("00000000000000000000006f");

        ObjectId expectedOwnerId = new ObjectId(String.format("%024x", userId));
        assertThat(saved.getOwnerId()).isEqualTo(expectedOwnerId);

        // AI must be called exactly once
        verify(restTemplate, times(1))
                .postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class));
    }

    // ==============================================================
    // FAILURE CASE: AI returns false → throw exception, do NOT save
    // ==============================================================

    @Test
    void testAddPost_whenHateSpeech_shouldThrowException() {
        AddPostDto dto = new AddPostDto(
                new ObjectId("00000000000000000000006f"),
                "Bad Title",
                "Some hateful text"
        );
        Long userId = 5L;

        // Mock AI saying "this is hate speech"
        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(false, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        // Expect exception
        assertThatThrownBy(() -> postService.addPost(dto, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("There is hate speech");

        // Ensure DB remains empty
        assertThat(postRepository.count()).isZero();
    }

    // ==============================================================
    // Validate the EXACT JSON body sent to FastAPI
    // ==============================================================

    @Test
    void testAnalyzeText_shouldSendCorrectJsonToAi() {
        String text = "hello \"world\"";

        // Mock AI response
        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(true, HttpStatus.OK);
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        // When
        boolean result = postService.analyzeText(text);

        // Then
        assertThat(result).isTrue();

        // Capture the EXACT body sent to the API
        verify(restTemplate).postForEntity(eq(url), captor.capture(), eq(Boolean.class));

        HttpEntity captured = captor.getValue();
        String jsonSent = (String) captured.getBody();

        // The JSON must be EXACT
        assertThat(jsonSent).isEqualTo("{\"text\":\"hello \\\"world\\\"\"}");

        // Ensure correct Content-Type header
        HttpHeaders headers = captured.getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

}
