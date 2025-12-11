package org.example.backend.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@WithMockUser
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    private Long userId;
    private ObjectId postId;
    private ObjectId commentId;
    private AddCommentDTO addCommentDTO;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        userId = 123456L;
        postId = new ObjectId();
        commentId = new ObjectId();

        addCommentDTO = AddCommentDTO.builder()
                .postId(postId)
                .parentId(null)
                .content("Test comment content")
                .build();

        testComment = Comment.builder()
                .id(commentId)
                .postId(postId)
                .content("Test comment content")
                .build();
    }


    @Test
    void createComment_DeletedPost_Returns500() throws Exception {
        when(commentService.addComment(anyLong(), any(AddCommentDTO.class)))
                .thenThrow(new IllegalStateException("this post is deleted"));

        mockMvc.perform(post("/api/comment/v1/create-comment")
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCommentDTO)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void createComment_EmptyContent_Success() throws Exception {
        addCommentDTO.setContent("");
        when(commentService.addComment(anyLong(), any(AddCommentDTO.class)))
                .thenReturn(testComment);

        mockMvc.perform(post("/api/comment/v1/create-comment")
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCommentDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void createComment_WithoutUserId_CallsServiceWithNull() throws Exception {
        when(commentService.addComment(isNull(), any(AddCommentDTO.class)))
                .thenReturn(testComment);

        mockMvc.perform(post("/api/comment/v1/create-comment")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCommentDTO)))
                .andExpect(status().isOk());

        verify(commentService).addComment(isNull(), any(AddCommentDTO.class));
    }

    @Test
    void deleteComment_Success() throws Exception {
        doNothing().when(commentService).deleteComment(any(ObjectId.class), anyLong());

        mockMvc.perform(delete("/api/comment/v1/delete-comment/{commentId}", commentId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("Comment deleted successfully"));

        verify(commentService).deleteComment(eq(commentId), eq(userId));
    }

    @Test
    void deleteComment_Unauthorized_Returns500() throws Exception {
        doThrow(new AccessDeniedException("User cannot delete this post"))
                .when(commentService).deleteComment(any(ObjectId.class), anyLong());

        mockMvc.perform(delete("/api/comment/v1/delete-comment/{commentId}", commentId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId))
                .andExpect(status().is5xxServerError());
    }


}