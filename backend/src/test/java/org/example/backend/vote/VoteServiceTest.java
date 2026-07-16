package org.example.backend.vote;

import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock private PostVoteRepository postVoteRepository;
    @Mock private CommentVoteRepository commentVoteRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;

    @InjectMocks
    private VoteService voteService;

    private UUID postId;
    private UUID commentId;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        commentId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Post activePost(UUID id) {
        Post post = new Post();
        post.setId(id);
        post.setIsDeleted(false);
        return post;
    }

    private Post deletedPost(UUID id) {
        Post post = new Post();
        post.setId(id);
        post.setIsDeleted(true);
        return post;
    }

    private Comment activeComment(UUID id) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setIsDeleted(false);
        return comment;
    }

    private Comment deletedComment(UUID id) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setIsDeleted(true);
        return comment;
    }

    private VoteDTO voteDTO(UUID targetId, int value) {
        return VoteDTO.builder().targetId(targetId).value(value).build();
    }

    private UpdateVoteDTO updateVoteDTO(UUID targetId, int value) {
        UpdateVoteDTO dto = new UpdateVoteDTO();
        dto.setTargetId(targetId);
        dto.setValue(value);
        return dto;
    }

    // =========================================================================
    // vote() — POST target type
    // =========================================================================

    @Test
    void vote_Post_NewVote_SavesNewRow() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(activePost(postId)));
        when(postVoteRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());

        voteService.vote(voteDTO(postId, 1), VoteTargetType.POST, userId);

        ArgumentCaptor<PostVote> captor = ArgumentCaptor.forClass(PostVote.class);
        verify(postVoteRepository).save(captor.capture());
        PostVote saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getPostId()).isEqualTo(postId);
        assertThat(saved.getVoteType()).isEqualTo(1);
    }

    @Test
    void vote_Post_ExistingVote_UpdatesVoteType() {
        PostVote existing = PostVote.builder().userId(userId).postId(postId).voteType(1).build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(activePost(postId)));
        when(postVoteRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(existing));

        voteService.vote(voteDTO(postId, -1), VoteTargetType.POST, userId);

        ArgumentCaptor<PostVote> captor = ArgumentCaptor.forClass(PostVote.class);
        verify(postVoteRepository).save(captor.capture());
        assertThat(captor.getValue().getVoteType()).isEqualTo(-1);
    }

    @Test
    void vote_Post_PostNotFound_ThrowsIllegalArgument() {
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.vote(voteDTO(postId, 1), VoteTargetType.POST, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(postId.toString());
    }

    @Test
    void vote_Post_DeletedPost_ThrowsIllegalState() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(deletedPost(postId)));

        assertThatThrownBy(() -> voteService.vote(voteDTO(postId, 1), VoteTargetType.POST, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deleted post");
    }

    // =========================================================================
    // vote() — COMMENT target type
    // =========================================================================

    @Test
    void vote_Comment_NewVote_SavesNewRow() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(activeComment(commentId)));
        when(commentVoteRepository.findByUserIdAndCommentId(userId, commentId)).thenReturn(Optional.empty());

        voteService.vote(voteDTO(commentId, 1), VoteTargetType.COMMENT, userId);

        ArgumentCaptor<CommentVote> captor = ArgumentCaptor.forClass(CommentVote.class);
        verify(commentVoteRepository).save(captor.capture());
        CommentVote saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getCommentId()).isEqualTo(commentId);
        assertThat(saved.getVoteType()).isEqualTo(1);
    }

    @Test
    void vote_Comment_DeletedComment_ThrowsIllegalState() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(deletedComment(commentId)));

        assertThatThrownBy(() -> voteService.vote(voteDTO(commentId, 1), VoteTargetType.COMMENT, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deleted comment");
    }

    // =========================================================================
    // updateVote()
    // =========================================================================

    @Test
    void updateVote_PostVoteFound_UpdatesAndSaves() {
        PostVote existing = PostVote.builder().userId(userId).postId(postId).voteType(1).build();
        when(postVoteRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(existing));
        when(postRepository.findById(postId)).thenReturn(Optional.of(activePost(postId)));

        voteService.updateVote(updateVoteDTO(postId, -1), userId);

        ArgumentCaptor<PostVote> captor = ArgumentCaptor.forClass(PostVote.class);
        verify(postVoteRepository).save(captor.capture());
        assertThat(captor.getValue().getVoteType()).isEqualTo(-1);
        verifyNoInteractions(commentVoteRepository);
    }

    @Test
    void updateVote_CommentVoteFound_UpdatesAndSaves() {
        CommentVote existing = CommentVote.builder().userId(userId).commentId(commentId).voteType(-1).build();
        when(postVoteRepository.findByUserIdAndPostId(userId, commentId)).thenReturn(Optional.empty());
        when(commentVoteRepository.findByUserIdAndCommentId(userId, commentId)).thenReturn(Optional.of(existing));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(activeComment(commentId)));

        voteService.updateVote(updateVoteDTO(commentId, 1), userId);

        ArgumentCaptor<CommentVote> captor = ArgumentCaptor.forClass(CommentVote.class);
        verify(commentVoteRepository).save(captor.capture());
        assertThat(captor.getValue().getVoteType()).isEqualTo(1);
    }

    @Test
    void updateVote_NeitherVoteFound_ThrowsIllegalArgument() {
        UUID unknownId = UUID.randomUUID();
        when(postVoteRepository.findByUserIdAndPostId(userId, unknownId)).thenReturn(Optional.empty());
        when(commentVoteRepository.findByUserIdAndCommentId(userId, unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.updateVote(updateVoteDTO(unknownId, 1), userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vote not found");
    }

    // =========================================================================
    // deleteVote()
    // =========================================================================

    @Test
    void deleteVote_PostVoteFound_DeletesRow() {
        PostVote existing = PostVote.builder().userId(userId).postId(postId).voteType(1).build();
        when(postVoteRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(existing));

        voteService.deleteVote(postId, userId);

        verify(postVoteRepository).delete(existing);
        verifyNoInteractions(commentVoteRepository);
    }

    @Test
    void deleteVote_CommentVoteFound_DeletesRow() {
        CommentVote existing = CommentVote.builder().userId(userId).commentId(commentId).voteType(-1).build();
        when(postVoteRepository.findByUserIdAndPostId(userId, commentId)).thenReturn(Optional.empty());
        when(commentVoteRepository.findByUserIdAndCommentId(userId, commentId)).thenReturn(Optional.of(existing));

        voteService.deleteVote(commentId, userId);

        verify(commentVoteRepository).delete(existing);
    }

    @Test
    void deleteVote_NeitherVoteFound_ThrowsIllegalArgument() {
        UUID unknownId = UUID.randomUUID();
        when(postVoteRepository.findByUserIdAndPostId(userId, unknownId)).thenReturn(Optional.empty());
        when(commentVoteRepository.findByUserIdAndCommentId(userId, unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.deleteVote(unknownId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vote not found");
    }

    // =========================================================================
    // isVote()
    // =========================================================================

    @Test
    void isVote_PostVoteExists_ReturnsVoteType() {
        PostVote existing = PostVote.builder().userId(userId).postId(postId).voteType(1).build();
        when(postVoteRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(existing));

        Integer result = voteService.isVote(postId, userId);

        assertThat(result).isEqualTo(1);
        verify(commentVoteRepository, never()).findByUserIdAndCommentId(any(), any());
    }

    @Test
    void isVote_CommentVoteExists_ReturnsVoteType() {
        CommentVote existing = CommentVote.builder().userId(userId).commentId(commentId).voteType(-1).build();
        when(postVoteRepository.findByUserIdAndPostId(userId, commentId)).thenReturn(Optional.empty());
        when(commentVoteRepository.findByUserIdAndCommentId(userId, commentId)).thenReturn(Optional.of(existing));

        Integer result = voteService.isVote(commentId, userId);

        assertThat(result).isEqualTo(-1);
    }

    @Test
    void isVote_NoVoteExists_ReturnsZero() {
        UUID unknownId = UUID.randomUUID();
        when(postVoteRepository.findByUserIdAndPostId(userId, unknownId)).thenReturn(Optional.empty());
        when(commentVoteRepository.findByUserIdAndCommentId(userId, unknownId)).thenReturn(Optional.empty());

        Integer result = voteService.isVote(unknownId, userId);

        assertThat(result).isEqualTo(0);
    }
}
