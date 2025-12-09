package org.example.backend.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostControllerTest {

    private MockMvc mockMvc;
    private PostService postService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() throws Exception {
        objectMapper = new ObjectMapper();
        postService = mock(PostService.class);

        PostController postController = new PostController();

        // manually inject service
        var field = PostController.class.getDeclaredField("postService");
        field.setAccessible(true);
        field.set(postController, postService);

        // add a global exception handler for standaloneSetup
        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testAddPost_success() throws Exception {
        AddPostDto dto = AddPostDto.builder()
                .title("Hello")
                .content("Clean content here")
                .forumId(null)
                .build();

        doNothing().when(postService).addPost(any(AddPostDto.class), eq(10L));

        mockMvc.perform(post("/api/post/v1/post")
                        .requestAttr("userId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("")); // empty body
    }

    @Test
    void testAddPost_hateSpeech() throws Exception {
        AddPostDto dto = AddPostDto.builder()
                .title("Bad")
                .content("Hate speech content")
                .forumId(null)
                .build();

        doThrow(new HateSpeechException("hate speech detected"))
                .when(postService).addPost(any(AddPostDto.class), eq(10L));

        mockMvc.perform(post("/api/post/v1/post")
                        .requestAttr("userId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("hate speech detected"));
    }

    @Test
    void testAddPost_internalServerError() throws Exception {
        AddPostDto dto = AddPostDto.builder()
                .title("Title")
                .content("Some content")
                .forumId(null)
                .build();

        doThrow(new RuntimeException("unexpected failure"))
                .when(postService).addPost(any(AddPostDto.class), eq(10L));

        mockMvc.perform(post("/api/post/v1/post")
                        .requestAttr("userId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Internal Server Error"));
    }

    // -------------------
    // Exception handler for standaloneSetup
    // -------------------
    @ControllerAdvice
    static class GlobalExceptionHandler {

        @ExceptionHandler(HateSpeechException.class)
        public ResponseEntity<String> handleHateSpeech(HateSpeechException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<String> handleOtherExceptions(Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error");
        }
    }
}
