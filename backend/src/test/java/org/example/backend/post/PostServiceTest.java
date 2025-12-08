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
    private RestTemplate restTemplate;   // Mock external AI service

    private final String url = "http://localhost:8000/api/hate/v1/analyze";

    // -------------------------------------------
    // SUCCESS CASE: NO HATE SPEECH -> POST SAVED
    // -------------------------------------------

    @Test
    void testAddPost_whenCleanText_shouldSavePost() {
        // Given
        AddPostDto dto = new AddPostDto(new ObjectId("6755dc881c3dfd780253e420"), "Test Title", "Normal content");
        Long userId = 5L;

        // Mock the AI service returning "true"
        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(true, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        // When
        postService.addPost(dto, userId);

        // Then
        Post saved = postRepository.findAll().stream().findFirst().orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Test Title");
        assertThat(saved.getForumId()).isEqualTo(111L);
        assertThat(saved.getContent()).isEqualTo("Normal content");
        assertThat(saved.getOwnerId()).isEqualTo(new ObjectId(String.format("%024x", userId)));

        // Also verify REST was called exactly once
        verify(restTemplate, times(1))
                .postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class));
    }

    // -------------------------------------------------------
    // FAILURE CASE: HATE SPEECH DETECTED -> EXCEPTION THROWN
    // -------------------------------------------------------
    @Test
    void testAddPost_whenHateSpeech_shouldThrowException() {
        // Given
        AddPostDto dto = new AddPostDto(new ObjectId("6755dc881c3dfd780253e420"), "Bad Title", "Some hateful text");
        Long userId = 5L;

        // Mock AI service returns false (hate speech detected)
        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(false, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        // When / Then
        assertThatThrownBy(() -> postService.addPost(dto, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("There is hate speech");

        // Ensure nothing saved to DB
        assertThat(postRepository.count()).isZero();
    }

    // -------------------------------------------------------
    // VALIDATE JSON BODY SENT TO AI SERVICE
    // -------------------------------------------------------
    @Test
    void testAnalyzeText_shouldSendCorrectJsonToAi() {
        // Given
        String text = "hello \"world\"";

        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(true, HttpStatus.OK);
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        // When
        boolean result = postService.analyzeText(text);

        // Then
        assertThat(result).isTrue();

        verify(restTemplate).postForEntity(eq(url), captor.capture(), eq(Boolean.class));

        HttpEntity captured = captor.getValue();
        String json = (String) captured.getBody();

        assertThat(json).isEqualTo("{\"text\":\"hello \\\"world\\\"\"}");

        // Ensure content type header set
        HttpHeaders headers = captured.getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

}
