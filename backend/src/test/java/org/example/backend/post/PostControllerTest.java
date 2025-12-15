package org.example.backend.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
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

    // -------------------
    // addPost tests
    // -------------------
    @Test
    void testAddPost_success() throws Exception {
        AddPostDto dto = AddPostDto.builder()
                .title("Hello")
                .content("Clean content here")
                .forumId(null)
                .build();

        Post mockPost = Post.builder().id(new ObjectId()).build();
        when(postService.addPost(any(AddPostDto.class), eq(10L))).thenReturn(mockPost);

        mockMvc.perform(post("/api/post/v1/post")
                        .requestAttr("userId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string(mockPost.getId().toHexString()));
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
    // updatePost tests
    // -------------------
    @Test
    void testUpdatePost_success() throws Exception {
        // Prepare the DTO for updating the post
        AddPostDto dto = AddPostDto.builder()
                .title("Updated Title")
                .content("Updated content here")
                .forumId(null) // Optional for update
                .build();

        // Example post ID to update
        ObjectId postId = new ObjectId();

        // Mock the service to return a Post object (or do nothing if your service returns void)
        Post updatedPost = Post.builder().id(postId).build();
        when(postService.updatePost(eq(postId), any(AddPostDto.class), eq(10L)))
                .thenReturn(updatedPost);

        // Perform PUT request
        mockMvc.perform(put("/api/post/v1/post/{postId}", postId)
                        .requestAttr("userId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string(postId.toHexString())); // Expect the controller to return postId
    }


    @Test
    void testUpdatePost_hateSpeech() throws Exception {

        ObjectId postId = new ObjectId();

        AddPostDto dto = AddPostDto.builder()
                .title("Bad update")
                .content("Hate speech content")
                .forumId(null)
                .build();

        doThrow(new HateSpeechException("hate speech detected"))
                .when(postService).updatePost(eq(postId), any(AddPostDto.class), eq(10L));

        mockMvc.perform(put("/api/post/v1/post/{postId}", postId)
                        .requestAttr("userId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("hate speech detected"));
    }

    @Test
    void testUpdatePost_internalServerError() throws Exception {
        AddPostDto dto = AddPostDto.builder()
                .title("Title")
                .content("Some content")
                .forumId(null)
                .build();

        ObjectId postId = new ObjectId();

        doThrow(new RuntimeException("unexpected failure"))
                .when(postService).updatePost(eq(postId), any(AddPostDto.class), eq(10L));

        mockMvc.perform(put("/api/post/v1/post/{postId}", postId)  // <-- use path variable
                        .requestAttr("userId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Internal Server Error"));
    }

    // -------------------
// deletePost tests
// -------------------
    @Test
    void testDeletePost_success() throws Exception {
        ObjectId postId = new ObjectId();

        // The service method should not throw -> means "success"
        doNothing().when(postService).deletePost(postId, 10L);

        mockMvc.perform(delete("/api/post/v1/post/{postId}", postId)
                        .requestAttr("userId", 10L))
                .andExpect(status().isOk())
                .andExpect(content().string("deleted"));
    }
    @Test
    void testDeletePost_internalServerError() throws Exception {
        ObjectId postId = new ObjectId();

        doThrow(new RuntimeException("unexpected failure"))
                .when(postService).deletePost(eq(postId), eq(10L));

        mockMvc.perform(delete("/api/post/v1/post/{postId}", postId)  // <-- path variable
                        .requestAttr("userId", 10L))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Internal Server Error"));
    }

    
    // -------------------
    // Exception handler
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
