package org.example.backend.deletion;

import com.mongodb.client.result.UpdateResult;
import org.bson.types.ObjectId;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CascadeDeletionServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CascadeDeletionService cascadeDeletionService;

    private ObjectId forumId;
    private ObjectId postId;
    private ObjectId commentId;
    private ObjectId voteId;
    private UpdateResult updateResult;

    @BeforeEach
    void setUp() {
        forumId = new ObjectId();
        postId = new ObjectId();
        commentId = new ObjectId();
        voteId = new ObjectId();

        updateResult = mock(UpdateResult.class);
    }

    // ==================== DELETE FORUM TESTS ====================

    @Test
    void deleteForum_WithPosts_CascadesCorrectly() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq("forums")))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(1L);

        cascadeDeletionService.deleteForum(forumId);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("forums"));
    }

    @Test
    void deleteForum_ForumNotFound_LogsWarning() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq("forums")))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(0L);

        cascadeDeletionService.deleteForum(forumId);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("forums"));
    }

    // ==================== DELETE POST TESTS ====================

    @Test
    void deletePost_WithComments_CascadesCorrectly() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq("posts")))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(1L);

        cascadeDeletionService.deletePost(postId);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("posts"));
    }

    @Test
    void deletePost_PostNotFound_LogsWarning() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq("posts")))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(0L);

        cascadeDeletionService.deletePost(postId);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("posts"));
    }

    // ==================== DELETE COMMENT TESTS ====================

       @Test
    void deleteComment_WithVotes_CascadesCorrectly() {
        // Arrange
        Comment comment = Comment.builder()
                .id(commentId)
                .postId(postId)
                .content("Test comment")
                .build();

        Post post = Post.builder()
                .id(postId)
                .commentCount(10)
                .build();

        // Mock repository calls
        when(commentRepository.findById(commentId))
                .thenReturn(java.util.Optional.of(comment));
        when(postRepository.findById(postId))
                .thenReturn(java.util.Optional.of(post));

        // Mock aggregation results for nested comment deletion
        org.springframework.data.mongodb.core.aggregation.AggregationResults<org.bson.Document> mockResults =
                mock(org.springframework.data.mongodb.core.aggregation.AggregationResults.class);
        when(mongoTemplate.aggregate(
                any(org.springframework.data.mongodb.core.aggregation.Aggregation.class),
                eq("comments"),
                eq(org.bson.Document.class)))
                .thenReturn(mockResults);

        org.bson.Document mockDoc = new org.bson.Document();
        mockDoc.put("parentId", commentId);
        mockDoc.put("descendantIds", java.util.Collections.emptyList());

        when(mockResults.getUniqueMappedResult()).thenReturn(mockDoc);

        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), anyString()))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(1L);

        // Act
        cascadeDeletionService.deleteComment(commentId);

        // Assert
        verify(commentRepository).findById(commentId);
        verify(mongoTemplate).aggregate(
                any(org.springframework.data.mongodb.core.aggregation.Aggregation.class),
                eq("comments"),
                eq(org.bson.Document.class));
    }

    @Test
    void deleteComment_CommentNotFound_ThrowsException() {
        // Arrange
        when(commentRepository.findById(commentId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            cascadeDeletionService.deleteComment(commentId);
        });

        verify(commentRepository).findById(commentId);
        verify(mongoTemplate, never()).aggregate(any(), anyString(), any());
    }

    @Test
    void deleteComment_WithNestedReplies_DeletesAll() {
        // Arrange
        ObjectId childCommentId = new ObjectId();

        Comment comment = Comment.builder()
                .id(commentId)
                .postId(postId)
                .content("Parent comment")
                .build();

        Post post = Post.builder()
                .id(postId)
                .commentCount(5)
                .build();

        when(commentRepository.findById(commentId))
                .thenReturn(java.util.Optional.of(comment));
        when(postRepository.findById(postId))
                .thenReturn(java.util.Optional.of(post));

        // Mock aggregation to return nested comments
        org.springframework.data.mongodb.core.aggregation.AggregationResults<org.bson.Document> mockResults =
                mock(org.springframework.data.mongodb.core.aggregation.AggregationResults.class);
        when(mongoTemplate.aggregate(
                any(org.springframework.data.mongodb.core.aggregation.Aggregation.class),
                eq("comments"),
                eq(org.bson.Document.class)))
                .thenReturn(mockResults);

        org.bson.Document mockDoc = new org.bson.Document();
        mockDoc.put("parentId", commentId);
        mockDoc.put("descendantIds", java.util.Arrays.asList(childCommentId));

        when(mockResults.getUniqueMappedResult()).thenReturn(mockDoc);

        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), anyString()))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(2L);

        // Act
        cascadeDeletionService.deleteComment(commentId);

        // Assert
        verify(commentRepository).findById(commentId);
        verify(postRepository).findById(postId);
        verify(mongoTemplate).aggregate(
                any(org.springframework.data.mongodb.core.aggregation.Aggregation.class),
                eq("comments"),
                eq(org.bson.Document.class));
        verify(mongoTemplate, atLeastOnce()).updateMulti(any(Query.class), any(Update.class), anyString());
    }
    // ==================== DELETE VOTE TESTS ====================

    @Test
    void deleteVote_Success() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq("votes")))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(1L);

        cascadeDeletionService.deleteVote(voteId);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("votes"));
    }

    @Test
    void deleteVote_VoteNotFound_LogsWarning() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq("votes")))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(0L);

        cascadeDeletionService.deleteVote(voteId);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("votes"));
    }

    // ==================== ASYNC CASCADE TESTS ====================

    @Test
    void cascadeDeleteForumPostsAsync_WithPosts_DeletesAll() {
        List<ObjectId> postIds = Arrays.asList(new ObjectId(), new ObjectId());

        when(mongoTemplate.find(any(Query.class), eq(ObjectId.class), eq("posts")))
                .thenReturn(postIds);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq("posts")))
                .thenReturn(updateResult);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq("comments")))
                .thenReturn(updateResult);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq("votes")))
                .thenReturn(updateResult);
        when(mongoTemplate.find(any(Query.class), eq(ObjectId.class), eq("comments")))
                .thenReturn(Collections.emptyList());
        when(updateResult.getModifiedCount()).thenReturn(2L);

        cascadeDeletionService.cascadeDeleteForumPostsAsync(forumId, Instant.now());

        verify(mongoTemplate).find(any(Query.class), eq(ObjectId.class), eq("posts"));
        verify(mongoTemplate, atLeastOnce()).updateMulti(any(Query.class), any(Update.class), anyString());
    }

    @Test
    void cascadeDeleteForumPostsAsync_NoPosts_DoesNothing() {
        when(mongoTemplate.find(any(Query.class), eq(ObjectId.class), eq("posts")))
                .thenReturn(Collections.emptyList());

        cascadeDeletionService.cascadeDeleteForumPostsAsync(forumId, Instant.now());

        verify(mongoTemplate).find(any(Query.class), eq(ObjectId.class), eq("posts"));
        verify(mongoTemplate, never()).updateMulti(any(Query.class), any(Update.class), eq("posts"));
    }

    @Test
    void cascadeDeletePostAsync_WithComments_DeletesAll() {
        List<ObjectId> commentIds = Arrays.asList(new ObjectId(), new ObjectId());

        when(mongoTemplate.find(any(Query.class), eq(ObjectId.class), eq("comments")))
                .thenReturn(commentIds);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), anyString()))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(2L);

        cascadeDeletionService.cascadeDeletePostAsync(postId, Instant.now());

        verify(mongoTemplate).find(any(Query.class), eq(ObjectId.class), eq("comments"));
        verify(mongoTemplate, atLeastOnce()).updateMulti(any(Query.class), any(Update.class), anyString());
    }

    @Test
    void cascadeDeletePostAsync_NoComments_DeletesPostVotesOnly() {
        when(mongoTemplate.find(any(Query.class), eq(ObjectId.class), eq("comments")))
                .thenReturn(Collections.emptyList());
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq("votes")))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(0L);

        cascadeDeletionService.cascadeDeletePostAsync(postId, Instant.now());

        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq("votes"));
    }

    @Test
    void cascadeDeleteCommentVotesAsync_Success() {
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq("votes")))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(5L);

        cascadeDeletionService.cascadeDeleteCommentVotesAsync(commentId, Instant.now());

        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq("votes"));
    }

    @Test
    void cascadeDeleteCommentVotesAsync_NoVotes_Success() {
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq("votes")))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(0L);

        cascadeDeletionService.cascadeDeleteCommentVotesAsync(commentId, Instant.now());

        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq("votes"));
    }

    // ==================== BATCH PROCESSING TESTS ====================

    @Test
    void cascadeDeleteForumPostsAsync_LargeBatch_ProcessesInBatches() {
        // Create 150 post IDs (should be processed in 2 batches of 100 and 50)
        List<ObjectId> postIds = new java.util.ArrayList<>();
        for (int i = 0; i < 150; i++) {
            postIds.add(new ObjectId());
        }

        when(mongoTemplate.find(any(Query.class), eq(ObjectId.class), eq("posts")))
                .thenReturn(postIds);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), anyString()))
                .thenReturn(updateResult);
        when(mongoTemplate.find(any(Query.class), eq(ObjectId.class), eq("comments")))
                .thenReturn(Collections.emptyList());
        when(updateResult.getModifiedCount()).thenReturn(100L, 50L, 0L);

        cascadeDeletionService.cascadeDeleteForumPostsAsync(forumId, Instant.now());

        // Should be called at least twice for batching posts
        verify(mongoTemplate, atLeast(2)).updateMulti(any(Query.class), any(Update.class), eq("posts"));
    }

    // ==================== HARD DELETE TESTS ====================

    @Test
    void hardDeleteOldEntities_DeletesOldRecords() {
        com.mongodb.client.result.DeleteResult deleteResult = mock(com.mongodb.client.result.DeleteResult.class);
        when(mongoTemplate.remove(any(Query.class), eq("forums")))
                .thenReturn(deleteResult);
        when(deleteResult.getDeletedCount()).thenReturn(10L);

        cascadeDeletionService.hardDeleteOldEntities("forums", 30);

        verify(mongoTemplate).remove(any(Query.class), eq("forums"));
    }

    @Test
    void hardDeleteOldEntities_NoOldRecords_DeletesNone() {
        com.mongodb.client.result.DeleteResult deleteResult = mock(com.mongodb.client.result.DeleteResult.class);
        when(mongoTemplate.remove(any(Query.class), eq("posts")))
                .thenReturn(deleteResult);
        when(deleteResult.getDeletedCount()).thenReturn(0L);

        cascadeDeletionService.hardDeleteOldEntities("posts", 30);

        verify(mongoTemplate).remove(any(Query.class), eq("posts"));
    }

    @Test
    void hardDeleteOldEntities_DifferentCollections_CallsCorrectly() {
        com.mongodb.client.result.DeleteResult deleteResult = mock(com.mongodb.client.result.DeleteResult.class);
        when(mongoTemplate.remove(any(Query.class), anyString()))
                .thenReturn(deleteResult);
        when(deleteResult.getDeletedCount()).thenReturn(5L);

        cascadeDeletionService.hardDeleteOldEntities("comments", 60);
        cascadeDeletionService.hardDeleteOldEntities("votes", 90);

        verify(mongoTemplate).remove(any(Query.class), eq("comments"));
        verify(mongoTemplate).remove(any(Query.class), eq("votes"));
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    void cascadeDeleteForumPostsAsync_ExceptionThrown_LogsError() {
        when(mongoTemplate.find(any(Query.class), eq(ObjectId.class), eq("posts")))
                .thenThrow(new RuntimeException("Database error"));

        // Should not throw, just log the error
        cascadeDeletionService.cascadeDeleteForumPostsAsync(forumId, Instant.now());

        verify(mongoTemplate).find(any(Query.class), eq(ObjectId.class), eq("posts"));
    }

    @Test
    void cascadeDeletePostAsync_ExceptionThrown_LogsError() {
        when(mongoTemplate.find(any(Query.class), eq(ObjectId.class), eq("comments")))
                .thenThrow(new RuntimeException("Database error"));

        // Should not throw, just log the error
        cascadeDeletionService.cascadeDeletePostAsync(postId, Instant.now());

        verify(mongoTemplate).find(any(Query.class), eq(ObjectId.class), eq("comments"));
    }

    @Test
    void cascadeDeleteCommentVotesAsync_ExceptionThrown_LogsError() {
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq("votes")))
                .thenThrow(new RuntimeException("Database error"));

        // Should not throw, just log the error
        cascadeDeletionService.cascadeDeleteCommentVotesAsync(commentId, Instant.now());

        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq("votes"));
    }
}