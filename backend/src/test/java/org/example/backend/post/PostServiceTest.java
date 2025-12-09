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
import org.springframework.http.*;

@SpringBootTest
class PostServiceTest extends AbstractMongoIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @MockBean
    private RestTemplate restTemplate; // mock external API

    private final String url = "http://localhost:8000/api/hate/v1/analyze";

    // ---------------------------
    // SUCCESS CASE: addPost
    // ---------------------------
    @Test
    void testAddPost_whenCleanText_shouldSavePost() {
        ObjectId forumId = new ObjectId("00000000000000000000006f");
        AddPostDto dto = new AddPostDto(forumId, "Test Title", "Normal content");
        Long userId = 5L;

        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(true, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        Post savedPost = postService.addPost(dto, userId);

        // Verify DB contains the post
        Post fromDb = postRepository.findById(savedPost.getId()).orElse(null);
        assertThat(fromDb).isNotNull();
        assertThat(fromDb.getTitle()).isEqualTo("Test Title");
        assertThat(fromDb.getContent()).isEqualTo("Normal content");
        assertThat(fromDb.getForumId()).isEqualTo(forumId);
        assertThat(fromDb.getOwnerId()).isEqualTo(new ObjectId(String.format("%024x", userId)));

        verify(restTemplate, times(1))
                .postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class));
    }

    // ---------------------------
    // FAILURE CASE: addPost with hate speech
    // ---------------------------
    @Test
    void testAddPost_whenHateSpeech_shouldThrowException() {
        AddPostDto dto = new AddPostDto(new ObjectId("00000000000000000000006f"),
                "Bad Title", "Some hateful text");
        Long userId = 5L;

        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(false, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        assertThatThrownBy(() -> postService.addPost(dto, userId))
                .isInstanceOf(HateSpeechException.class)
                .hasMessageContaining("hate speech detected");

        assertThat(postRepository.count()).isZero();
    }

    // ---------------------------
    // analyzeText sends correct JSON
    // ---------------------------
    @Test
    void testAnalyzeText_shouldSendCorrectJsonToAi() {
        String text = "hello \"world\"";

        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(true, HttpStatus.OK);
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        boolean result = postService.analyzeText(text);

        assertThat(result).isTrue();

        verify(restTemplate).postForEntity(eq(url), captor.capture(), eq(Boolean.class));
        HttpEntity captured = captor.getValue();
        String jsonSent = (String) captured.getBody();
        assertThat(jsonSent).isEqualTo("{\"text\":\"hello \\\"world\\\"\"}");

        HttpHeaders headers = captured.getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    // ---------------------------
    // SUCCESS CASE: updatePost
    // ---------------------------
    @Test
    void testUpdatePost_whenCleanText_shouldUpdatePost() {
        // Prepare existing post
        Long userId = 7L;
        ObjectId postId = new ObjectId();
        Post existingPost = Post.builder()
                .id(postId)
                .ownerId(new ObjectId(String.format("%024x", userId)))
                .title("Old Title")
                .content("Old Content")
                .isDeleted(false)
                .build();
        postRepository.save(existingPost);

        AddPostDto dto = new AddPostDto(null, "New Title", "New Content");

        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(true, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        Post updated = postService.updatePost(postId.toHexString(), dto, userId);

        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getContent()).isEqualTo("New Content");

        // Ensure DB also updated
        Post fromDb = postRepository.findById(postId).orElseThrow();
        assertThat(fromDb.getTitle()).isEqualTo("New Title");
        assertThat(fromDb.getContent()).isEqualTo("New Content");
    }

    // ---------------------------
    // FAILURE CASE: updatePost with hate speech
    // ---------------------------
    @Test
    void testUpdatePost_whenHateSpeech_shouldThrowException() {
        Long userId = 7L;
        ObjectId postId = new ObjectId();
        Post existingPost = Post.builder()
                .id(postId)
                .ownerId(new ObjectId(String.format("%024x", userId)))
                .title("Old Title")
                .content("Old Content")
                .isDeleted(false)
                .build();
        postRepository.save(existingPost);

        AddPostDto dto = new AddPostDto(null, "Bad Title", "Hateful Content");

        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(false, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        assertThatThrownBy(() -> postService.updatePost(postId.toHexString(), dto, userId))
                .isInstanceOf(HateSpeechException.class)
                .hasMessageContaining("hate speech detected");

        // Ensure post unchanged in DB
        Post fromDb = postRepository.findById(postId).orElseThrow();
        assertThat(fromDb.getTitle()).isEqualTo("Old Title");
        assertThat(fromDb.getContent()).isEqualTo("Old Content");
    }
}
