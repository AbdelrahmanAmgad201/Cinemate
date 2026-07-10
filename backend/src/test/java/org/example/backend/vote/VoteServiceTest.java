package org.example.backend.vote;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.backend.comment.Comment;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.post.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
                .targetType(VoteTargetType.POST)
                .voteType(1)
                .isDeleted(false)
                .build();

        voteDTO = VoteDTO.builder()
                        .targetId(postId)
                        .value(1)
                        .build();

        updateVoteDTO = new UpdateVoteDTO();
        updateVoteDTO.setTargetId(postId);
        updateVoteDTO.setValue(-1);
    }

    // ==================== helpers ====================

    // Vote counters are now updated atomically via MongoTemplate's $inc (REL-01) instead
    // of load-mutate-save, so these tests verify the delta sent to Mongo rather than a
    // mutated in-memory object.
    private void verifyPostUpdate(int upvoteDelta, int downvoteDelta, int scoreDelta) {
        ArgumentCaptor<Update> captor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), captor.capture(), eq(Post.class));
        assertIncDeltas(captor.getValue(), upvoteDelta, downvoteDelta, scoreDelta);
    }

    private void verifyCommentUpdate(int upvoteDelta, int downvoteDelta, int scoreDelta) {
        ArgumentCaptor<Update> captor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), captor.capture(), eq(Comment.class));
        assertIncDeltas(captor.getValue(), upvoteDelta, downvoteDelta, scoreDelta);
    }

    private void assertIncDeltas(Update update, int upvoteDelta, int downvoteDelta, int scoreDelta) {
        Document incDoc = (Document) update.getUpdateObject().get("$inc");
        assertEquals(upvoteDelta, incDoc.getInteger("upvoteCount"));
        assertEquals(downvoteDelta, incDoc.getInteger("downvoteCount"));
        assertEquals(scoreDelta, incDoc.getInteger("score"));
    }

    private void verifyNoMongoUpdate() {
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), any(Class.class));
    }

    // ==================== VOTE ON POST TESTS ====================

    @Test
    void vote_UpvotePost_Success() {
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        voteService.vote(voteDTO, VoteTargetType.POST, userId);

        verifyPostUpdate(1, 0, 1);
        verify(voteRepository).save(argThat(vote ->
                vote.getTargetId().equals(postId) &&
                        vote.getUserId().equals(userObjectId) &&
                        vote.getVoteType().equals(1) &&
                        vote.getTargetType() == VoteTargetType.POST
        ));
    }

    @Test
    void vote_DownvotePost_Success() {
        voteDTO.setValue(-1);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        voteService.vote(voteDTO, VoteTargetType.POST, userId);

        verifyPostUpdate(0, 1, -1);
        verify(voteRepository).save(argThat(vote ->
                vote.getVoteType().equals(-1)
        ));
    }

    @Test
    void vote_PostNotFound_ThrowsException() {
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> voteService.vote(voteDTO, VoteTargetType.POST, userId));

        verify(voteRepository, never()).save(any());
        verifyNoMongoUpdate();
    }

    @Test
    void vote_DeletedPost_ThrowsException() {
        testPost.setIsDeleted(true);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);

        assertThrows(IllegalStateException.class,
                () -> voteService.vote(voteDTO, VoteTargetType.POST, userId));

        verify(voteRepository, never()).save(any());
        verifyNoMongoUpdate();
    }

    // ==================== VOTE ON COMMENT TESTS ====================

    @Test
    void vote_UpvoteComment_Success() {
        voteDTO.setTargetId(commentId);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        voteService.vote(voteDTO, VoteTargetType.COMMENT, userId);

        verifyCommentUpdate(1, 0, 1);
        verify(voteRepository).save(argThat(vote ->
                vote.getTargetId().equals(commentId) &&
                        vote.getTargetType() == VoteTargetType.COMMENT
        ));
    }

    @Test
    void vote_DownvoteComment_Success() {
        voteDTO.setTargetId(commentId);
        voteDTO.setValue(-1);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        voteService.vote(voteDTO, VoteTargetType.COMMENT, userId);

        verifyCommentUpdate(0, 1, -1);
    }

    @Test
    void vote_CommentNotFound_ThrowsException() {
        voteDTO.setTargetId(commentId);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> voteService.vote(voteDTO, VoteTargetType.COMMENT, userId));

        verify(voteRepository, never()).save(any());
    }

    @Test
    void vote_DeletedComment_ThrowsException() {
        voteDTO.setTargetId(commentId);
        testComment.setIsDeleted(true);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);

        assertThrows(IllegalStateException.class,
                () -> voteService.vote(voteDTO, VoteTargetType.COMMENT, userId));

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

        // Should decrement upvote and increment downvote
        verifyPostUpdate(-1, 1, -2);
        assertEquals(-1, testVote.getVoteType());
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
        verifyPostUpdate(1, -1, 2);
        assertEquals(1, testVote.getVoteType());
    }

    @Test
    void updateVote_CommentVote_Success() {
        testVote.setTargetId(commentId);
        testVote.setTargetType(VoteTargetType.COMMENT);
        testVote.setVoteType(1);
        updateVoteDTO.setTargetId(commentId);

        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, commentId))
                .thenReturn(testVote);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        updateVoteDTO.setValue(-1);
        voteService.updateVote(updateVoteDTO, userId);

        verifyCommentUpdate(-1, 1, -2);
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

        verifyPostUpdate(-1, 0, -1);
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

        verifyPostUpdate(0, -1, 1);
        verify(deletionService).deleteVote(voteId);
    }

    @Test
    void deleteVote_CommentVote_Success() {
        testVote.setTargetId(commentId);
        testVote.setTargetType(VoteTargetType.COMMENT);
        testVote.setVoteType(1);
        when(voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(userObjectId, commentId))
                .thenReturn(testVote);
        when(accessService.canDeleteVote(userObjectId, voteId)).thenReturn(true);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        doNothing().when(deletionService).deleteVote(voteId);

        voteService.deleteVote(commentId, userId);

        verifyCommentUpdate(-1, 0, -1);
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

        voteService.vote(voteDTO, VoteTargetType.POST, userId);

        verify(voteRepository).save(argThat(vote ->
                vote.getUserId().equals(userObjectId) &&
                        vote.getTargetId().equals(postId) &&
                        vote.getVoteType().equals(1) &&
                        vote.getTargetType().equals(VoteTargetType.POST)
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

        // The service doesn't special-case "no actual change" — it always applies the
        // increment-upvote/decrement-downvote delta for the requested direction.
        verifyPostUpdate(1, -1, 2);
    }

    @Test
    void vote_ZeroVoteCounts_Increments() {
        testPost.setUpvoteCount(0);
        testPost.setDownvoteCount(0);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> i.getArgument(0));

        voteService.vote(voteDTO, VoteTargetType.POST, userId);

        // Atomic $inc deltas don't depend on the starting count.
        verifyPostUpdate(1, 0, 1);
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

        verifyPostUpdate(0, -1, 1);
    }
}
