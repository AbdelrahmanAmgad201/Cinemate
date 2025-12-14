package org.example.backend.vote;

import org.bson.types.ObjectId;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private CascadeDeletionService deletionService;

    @Mock
    private AccessService accessService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private VoteService voteService;

    private Long userId;
    private ObjectId userObjectId;
    private ObjectId postId;
    private ObjectId commentId;
    private ObjectId voteId;
    private Post testPost;
    private Comment testComment;
    private Vote testVote;
    private VoteDTO voteDTO;
    private UpdateVoteDTO updateVoteDTO;

    @BeforeEach
    void setUp() {
        userId = 123456L;
        userObjectId = new ObjectId(String.format("%024x", userId));
        postId = new ObjectId();
        commentId = new ObjectId();
        voteId = new ObjectId();

        testPost = Post.builder()
                .id(postId)
                .upvoteCount(5)
                .downvoteCount(2)
                .isDeleted(false)
                .build();

        testComment = Comment.builder()
                .id(commentId)
                .upvoteCount(3)
                .downvoteCount(1)
                .isDeleted(false)
                .build();

        testVote = Vote.builder()
                .id(voteId)
                .userId(userObjectId)
                .targetId(postId)
                .isPost(true)
                .voteType(1)
                .isDeleted(false)
                .build();

        voteDTO = new VoteDTO();
        voteDTO.setTargetId(postId);
        voteDTO.setValue(1);

        updateVoteDTO = new UpdateVoteDTO();
        updateVoteDTO.setTargetId(postId);
        updateVoteDTO.setValue(-1);
    }

    // ==================== VOTE ON POST TESTS ====================

    @Test
    void vote_UpvotePost_Success() {
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        voteService.vote(voteDTO, true, userId);

        assertEquals(6, testPost.getUpvoteCount());
        verify(postRepository).save(testPost);
        verify(voteRepository).save(argThat(vote ->
                vote.getTargetId().equals(postId) &&
                        vote.getUserId().equals(userObjectId) &&
                        vote.getVoteType().equals(1) &&
                        vote.getIsPost()
        ));
    }

    @Test
    void vote_DownvotePost_Success() {
        voteDTO.setValue(-1);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        voteService.vote(voteDTO, true, userId);

        assertEquals(3, testPost.getDownvoteCount());
        verify(postRepository).save(testPost);
        verify(voteRepository).save(argThat(vote ->
                vote.getVoteType().equals(-1)
        ));
    }

    @Test
    void vote_PostNotFound_ThrowsException() {
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> voteService.vote(voteDTO, true, userId));

        verify(voteRepository, never()).save(any());
        verify(postRepository, never()).save(any());
    }

    @Test
    void vote_DeletedPost_ThrowsException() {
        testPost.setIsDeleted(true);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);

        assertThrows(IllegalStateException.class,
                () -> voteService.vote(voteDTO, true, userId));

        verify(voteRepository, never()).save(any());
    }

    // ==================== VOTE ON COMMENT TESTS ====================

    @Test
    void vote_UpvoteComment_Success() {
        voteDTO.setTargetId(commentId);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        voteService.vote(voteDTO, false, userId);

        assertEquals(4, testComment.getUpvoteCount());
        verify(commentRepository).save(testComment);
        verify(voteRepository).save(argThat(vote ->
                vote.getTargetId().equals(commentId) &&
                        !vote.getIsPost()
        ));
    }

    @Test
    void vote_DownvoteComment_Success() {
        voteDTO.setTargetId(commentId);
        voteDTO.setValue(-1);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        voteService.vote(voteDTO, false, userId);

        assertEquals(2, testComment.getDownvoteCount());
        verify(commentRepository).save(testComment);
    }

    @Test
    void vote_CommentNotFound_ThrowsException() {
        voteDTO.setTargetId(commentId);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> voteService.vote(voteDTO, false, userId));

        verify(voteRepository, never()).save(any());
    }

    @Test
    void vote_DeletedComment_ThrowsException() {
        voteDTO.setTargetId(commentId);
        testComment.setIsDeleted(true);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);

        assertThrows(IllegalStateException.class,
                () -> voteService.vote(voteDTO, false, userId));

        verify(voteRepository, never()).save(any());
    }

    // ==================== UPDATE VOTE TESTS ====================

    @Test
    void updateVote_ChangeUpvoteToDownvote_Success() {
        testVote.setVoteType(1);
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(testVote);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        updateVoteDTO.setValue(-1);
        voteService.updateVote(updateVoteDTO, userId);

        // Should increment downvote and decrement upvote
        assertEquals(4, testPost.getUpvoteCount());
        assertEquals(3, testPost.getDownvoteCount());
        assertEquals(-1, testVote.getVoteType());
        verify(postRepository).save(testPost);
        verify(voteRepository).save(testVote);
    }

    @Test
    void updateVote_ChangeDownvoteToUpvote_Success() {
        testVote.setVoteType(-1);
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(testVote);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        updateVoteDTO.setValue(1);
        voteService.updateVote(updateVoteDTO, userId);

        // Should increment upvote and decrement downvote
        assertEquals(6, testPost.getUpvoteCount());
        assertEquals(1, testPost.getDownvoteCount());
        assertEquals(1, testVote.getVoteType());
        verify(postRepository).save(testPost);
    }

    @Test
    void updateVote_CommentVote_Success() {
        testVote.setTargetId(commentId);
        testVote.setIsPost(false);
        testVote.setVoteType(1);
        updateVoteDTO.setTargetId(commentId);

        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, commentId))
                .thenReturn(testVote);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        updateVoteDTO.setValue(-1);
        voteService.updateVote(updateVoteDTO, userId);

        assertEquals(2, testComment.getUpvoteCount());
        assertEquals(2, testComment.getDownvoteCount());
        verify(commentRepository).save(testComment);
    }

    @Test
    void updateVote_VoteNotFound_ThrowsException() {
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> voteService.updateVote(updateVoteDTO, userId));

        verify(voteRepository, never()).save(any());
    }

    @Test
    void updateVote_NotOwner_ThrowsAccessDeniedException() {
        testVote.setUserId(new ObjectId());
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(testVote);

        assertThrows(AccessDeniedException.class,
                () -> voteService.updateVote(updateVoteDTO, userId));

        verify(voteRepository, never()).save(any());
    }

    @Test
    void updateVote_TargetDeleted_ThrowsException() {
        testPost.setIsDeleted(true);
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(testVote);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);

        assertThrows(IllegalStateException.class,
                () -> voteService.updateVote(updateVoteDTO, userId));
    }

    // ==================== DELETE VOTE TESTS ====================

    @Test
    void deleteVote_Upvote_DecrementsCorrectly() {
        testVote.setVoteType(1);
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(testVote);
        when(accessService.canDeleteVote(userObjectId, voteId)).thenReturn(true);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        doNothing().when(deletionService).deleteVote(voteId);

        voteService.deleteVote(postId, userId);

        assertEquals(4, testPost.getUpvoteCount());
        verify(postRepository).save(testPost);
        verify(deletionService).deleteVote(voteId);
    }

    @Test
    void deleteVote_Downvote_DecrementsCorrectly() {
        testVote.setVoteType(-1);
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(testVote);
        when(accessService.canDeleteVote(userObjectId, voteId)).thenReturn(true);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        doNothing().when(deletionService).deleteVote(voteId);

        voteService.deleteVote(postId, userId);

        assertEquals(1, testPost.getDownvoteCount());
        verify(postRepository).save(testPost);
        verify(deletionService).deleteVote(voteId);
    }

    @Test
    void deleteVote_CommentVote_Success() {
        testVote.setTargetId(commentId);
        testVote.setIsPost(false);
        testVote.setVoteType(1);
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, commentId))
                .thenReturn(testVote);
        when(accessService.canDeleteVote(userObjectId, voteId)).thenReturn(true);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        doNothing().when(deletionService).deleteVote(voteId);

        voteService.deleteVote(commentId, userId);

        assertEquals(2, testComment.getUpvoteCount());
        verify(commentRepository).save(testComment);
        verify(deletionService).deleteVote(voteId);
    }

    @Test
    void deleteVote_VoteNotFound_ThrowsException() {
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> voteService.deleteVote(postId, userId));

        verify(deletionService, never()).deleteVote(any());
    }

    @Test
    void deleteVote_Unauthorized_ThrowsAccessDeniedException() {
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(testVote);
        when(accessService.canDeleteVote(userObjectId, voteId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> voteService.deleteVote(postId, userId));

        verify(deletionService, never()).deleteVote(any());
    }

    @Test
    void deleteVote_CallsDeletionService() {
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(testVote);
        when(accessService.canDeleteVote(userObjectId, voteId)).thenReturn(true);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        doNothing().when(deletionService).deleteVote(voteId);

        voteService.deleteVote(postId, userId);

        verify(deletionService).deleteVote(voteId);
    }

    // ==================== IS VOTE TESTS ====================

    @Test
    void isVote_VoteExists_ReturnsVoteType() {
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(testVote);

        Integer result = voteService.isVote(postId, userId);

        assertEquals(1, result);
    }

    @Test
    void isVote_VoteDoesNotExist_ReturnsZero() {
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(null);

        Integer result = voteService.isVote(postId, userId);

        assertEquals(0, result);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void vote_PreservesVoteData() {
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        voteService.vote(voteDTO, true, userId);

        verify(voteRepository).save(argThat(vote ->
                vote.getUserId().equals(userObjectId) &&
                        vote.getTargetId().equals(postId) &&
                        vote.getVoteType().equals(1) &&
                        vote.getIsPost().equals(true)
        ));
    }

    @Test
    void updateVote_FromUpvoteToUpvote_SameValue() {
        testVote.setVoteType(1);
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(testVote);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        updateVoteDTO.setValue(1); // Same as current
        voteService.updateVote(updateVoteDTO, userId);

        // Should still update (increment upvote, decrement downvote)
        assertEquals(6, testPost.getUpvoteCount());
        assertEquals(1, testPost.getDownvoteCount());
    }

    @Test
    void vote_ZeroVoteCounts_Increments() {
        testPost.setUpvoteCount(0);
        testPost.setDownvoteCount(0);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        voteService.vote(voteDTO, true, userId);

        assertEquals(1, testPost.getUpvoteCount());
        assertEquals(0, testPost.getDownvoteCount());
    }

    @Test
    void deleteVote_ZeroDownvoteCount_Decrements() {
        testPost.setDownvoteCount(0);
        testVote.setVoteType(-1);
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, postId))
                .thenReturn(testVote);
        when(accessService.canDeleteVote(userObjectId, voteId)).thenReturn(true);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        doNothing().when(deletionService).deleteVote(voteId);

        voteService.deleteVote(postId, userId);

        assertEquals(-1, testPost.getDownvoteCount());
        verify(postRepository).save(testPost);
    }
}