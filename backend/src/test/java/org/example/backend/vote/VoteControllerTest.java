package org.example.backend.vote;

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

@WebMvcTest(VoteController.class)
@WithMockUser
class VoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VoteService voteService;

    private Long userId;
    private ObjectId targetId;
    private ObjectId voteId;
    private VoteDTO voteDTO;
    private UpdateVoteDTO updateVoteDTO;

    @BeforeEach
    void setUp() {
        userId = 123456L;
        targetId = new ObjectId();
        voteId = new ObjectId();

        voteDTO = new VoteDTO();
        voteDTO.setTargetId(targetId);
        voteDTO.setValue(1);

        updateVoteDTO = new UpdateVoteDTO();
        updateVoteDTO.setId(voteId);
        updateVoteDTO.setValue(-1);
    }

    // ==================== POST VOTE TESTS ====================


    @Test
    void postVote_Downvote_Success() throws Exception {
        voteDTO.setValue(-1);
        doNothing().when(voteService).vote(any(VoteDTO.class), eq(true), anyLong());

        mockMvc.perform(post("/api/vote/v1/post-vote")
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteDTO)))
                .andExpect(status().isOk());

        verify(voteService).vote(argThat(dto ->
                dto.getValue().equals(-1)
        ), eq(true), eq(userId));
    }

    @Test
    void postVote_DeletedPost_Returns500() throws Exception {
        doThrow(new IllegalStateException("Cannot vote a deleted post"))
                .when(voteService).vote(any(VoteDTO.class), eq(true), anyLong());

        mockMvc.perform(post("/api/vote/v1/post-vote")
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteDTO)))
                .andExpect(status().is5xxServerError());
    }


    @Test
    void postVote_WithoutUserId_CallsServiceWithNull() throws Exception {
        doNothing().when(voteService).vote(any(VoteDTO.class), eq(true), isNull());

        mockMvc.perform(post("/api/vote/v1/post-vote")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteDTO)))
                .andExpect(status().isOk());

        verify(voteService).vote(any(VoteDTO.class), eq(true), isNull());
    }

    @Test
    void commentVote_Downvote_Success() throws Exception {
        voteDTO.setValue(-1);
        doNothing().when(voteService).vote(any(VoteDTO.class), eq(false), anyLong());

        mockMvc.perform(post("/api/vote/v1/comment-vote")
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteDTO)))
                .andExpect(status().isOk());

        verify(voteService).vote(any(VoteDTO.class), eq(false), eq(userId));
    }
    @Test
    void commentVote_DeletedComment_Returns500() throws Exception {
        doThrow(new IllegalStateException("Cannot vote a deleted comment"))
                .when(voteService).vote(any(VoteDTO.class), eq(false), anyLong());

        mockMvc.perform(post("/api/vote/v1/comment-vote")
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteDTO)))
                .andExpect(status().is5xxServerError());
    }


    @Test
    void commentVote_WithoutUserId_CallsServiceWithNull() throws Exception {
        doNothing().when(voteService).vote(any(VoteDTO.class), eq(false), isNull());

        mockMvc.perform(post("/api/vote/v1/comment-vote")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteDTO)))
                .andExpect(status().isOk());

        verify(voteService).vote(any(VoteDTO.class), eq(false), isNull());
    }

    // ==================== UPDATE VOTE TESTS ====================


    @Test
    void updateVote_NotOwner_Returns500() throws Exception {
        doThrow(new AccessDeniedException("User does not have permission"))
                .when(voteService).updateVote(any(UpdateVoteDTO.class), anyLong());

        mockMvc.perform(put("/api/vote/v1/update-vote")
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVoteDTO)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void updateVote_DeletedTarget_Returns500() throws Exception {
        doThrow(new IllegalStateException("Cannot vote a deleted post"))
                .when(voteService).updateVote(any(UpdateVoteDTO.class), anyLong());

        mockMvc.perform(put("/api/vote/v1/update-vote")
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVoteDTO)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void updateVote_WithoutUserId_CallsServiceWithNull() throws Exception {
        doNothing().when(voteService).updateVote(any(UpdateVoteDTO.class), isNull());

        mockMvc.perform(put("/api/vote/v1/update-vote")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVoteDTO)))
                .andExpect(status().isOk());

        verify(voteService).updateVote(any(UpdateVoteDTO.class), isNull());
    }

    // ==================== DELETE VOTE TESTS ====================

    @Test
    void deleteVote_Success() throws Exception {
        doNothing().when(voteService).deleteVote(any(ObjectId.class), anyLong());

        mockMvc.perform(delete("/api/vote/v1/delete-vote/{voteId}", voteId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId))
                .andExpect(status().isOk());

        verify(voteService).deleteVote(eq(voteId), eq(userId));
    }

    @Test
    void deleteVote_Unauthorized_Returns500() throws Exception {
        doThrow(new AccessDeniedException("User cannot delete this vote"))
                .when(voteService).deleteVote(any(ObjectId.class), anyLong());

        mockMvc.perform(delete("/api/vote/v1/delete-vote/{voteId}", voteId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void deleteVote_WithoutUserId_CallsServiceWithNull() throws Exception {
        doNothing().when(voteService).deleteVote(any(ObjectId.class), isNull());

        mockMvc.perform(delete("/api/vote/v1/delete-vote/{voteId}", voteId.toHexString())
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(voteService).deleteVote(eq(voteId), isNull());
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void postVote_ZeroValue_ProcessedByService() throws Exception {
        voteDTO.setValue(0);
        doNothing().when(voteService).vote(any(VoteDTO.class), eq(true), anyLong());

        mockMvc.perform(post("/api/vote/v1/post-vote")
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteDTO)))
                .andExpect(status().isOk());

        verify(voteService).vote(argThat(dto ->
                dto.getValue().equals(0)
        ), eq(true), eq(userId));
    }

    @Test
    void updateVote_ChangeToUpvote_Success() throws Exception {
        updateVoteDTO.setValue(1);
        doNothing().when(voteService).updateVote(any(UpdateVoteDTO.class), anyLong());

        mockMvc.perform(put("/api/vote/v1/update-vote")
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVoteDTO)))
                .andExpect(status().isOk());

        verify(voteService).updateVote(argThat(dto ->
                dto.getValue().equals(1)
        ), eq(userId));
    }
}