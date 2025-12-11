package org.example.backend.deletion;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.backend.comment.Comment;
import org.example.backend.forum.Forum;
import org.example.backend.post.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private AccessService accessService;

    private ObjectId userId;
    private ObjectId forumId;
    private ObjectId postId;
    private ObjectId commentId;
    private ObjectId voteId;
    private Forum testForum;
    private Post testPost;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        userId = new ObjectId();
        forumId = new ObjectId();
        postId = new ObjectId();
        commentId = new ObjectId();
        voteId = new ObjectId();

        testForum = new Forum();
        testForum.setId(forumId);
        testForum.setOwnerId(userId);
        testForum.setIsDeleted(false);

        testPost = Post.builder()
                .id(postId)
                .ownerId(userId)
                .forumId(forumId)
                .isDeleted(false)
                .build();

        testComment = Comment.builder()
                .id(commentId)
                .ownerId(userId)
                .postId(postId)
                .isDeleted(false)
                .build();
    }

    // ==================== FORUM DELETION TESTS ====================

    @Test
    void canDeleteForum_Owner_ReturnsTrue() {
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);

        boolean result = accessService.canDeleteForum(userId, forumId);

        assertTrue(result);
    }

    @Test
    void canDeleteForum_NotOwner_ReturnsFalse() {
        ObjectId differentUserId = new ObjectId();
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);

        boolean result = accessService.canDeleteForum(differentUserId, forumId);

        assertFalse(result);
    }

    @Test
    void canDeleteForum_ForumNotFound_ReturnsFalse() {
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(null);

        boolean result = accessService.canDeleteForum(userId, forumId);

        assertFalse(result);
    }

    @Test
    void canDeleteForum_ForumAlreadyDeleted_ReturnsFalse() {
        testForum.setIsDeleted(true);
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);

        boolean result = accessService.canDeleteForum(userId, forumId);

        assertFalse(result);
    }

    // ==================== POST DELETION TESTS ====================

    @Test
    void canDeletePost_PostOwner_ReturnsTrue() {
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);

        boolean result = accessService.canDeletePost(userId, postId);

        assertTrue(result);
    }

    @Test
    void canDeletePost_ForumOwner_ReturnsTrue() {
        ObjectId forumOwnerId = new ObjectId();
        testPost.setOwnerId(new ObjectId()); // Different post owner
        testForum.setOwnerId(forumOwnerId);

        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);

        boolean result = accessService.canDeletePost(forumOwnerId, postId);

        assertTrue(result);
    }

    @Test
    void canDeletePost_NotOwnerOrForumOwner_ReturnsFalse() {
        ObjectId differentUserId = new ObjectId();
        testPost.setOwnerId(new ObjectId());
        testForum.setOwnerId(new ObjectId());

        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);

        boolean result = accessService.canDeletePost(differentUserId, postId);

        assertFalse(result);
    }

    @Test
    void canDeletePost_PostNotFound_ReturnsFalse() {
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(null);

        boolean result = accessService.canDeletePost(userId, postId);

        assertFalse(result);
    }

    @Test
    void canDeletePost_PostAlreadyDeleted_ReturnsFalse() {
        testPost.setIsDeleted(true);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);

        boolean result = accessService.canDeletePost(userId, postId);

        assertFalse(result);
    }

    @Test
    void canDeletePost_ForumNotFound_ReturnsFalse() {
        testPost.setOwnerId(new ObjectId());
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(null);

        boolean result = accessService.canDeletePost(userId, postId);

        assertFalse(result);
    }

    // ==================== COMMENT DELETION TESTS ====================

    @Test
    void canDeleteComment_CommentOwner_ReturnsTrue() {
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);

        boolean result = accessService.canDeleteComment(userId, commentId);

        assertTrue(result);
    }

    @Test
    void canDeleteComment_PostOwner_ReturnsTrue() {
        ObjectId postOwnerId = new ObjectId();
        testComment.setOwnerId(new ObjectId());
        testPost.setOwnerId(postOwnerId);

        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);

        boolean result = accessService.canDeleteComment(postOwnerId, commentId);

        assertTrue(result);
    }

    @Test
    void canDeleteComment_ForumOwner_ReturnsTrue() {
        ObjectId forumOwnerId = new ObjectId();
        testComment.setOwnerId(new ObjectId());
        testPost.setOwnerId(new ObjectId());
        testForum.setOwnerId(forumOwnerId);

        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);

        boolean result = accessService.canDeleteComment(forumOwnerId, commentId);

        assertTrue(result);
    }

    @Test
    void canDeleteComment_NoPermission_ReturnsFalse() {
        ObjectId differentUserId = new ObjectId();
        testComment.setOwnerId(new ObjectId());
        testPost.setOwnerId(new ObjectId());
        testForum.setOwnerId(new ObjectId());

        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(testForum);

        boolean result = accessService.canDeleteComment(differentUserId, commentId);

        assertFalse(result);
    }

    @Test
    void canDeleteComment_CommentNotFound_ReturnsFalse() {
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(null);

        boolean result = accessService.canDeleteComment(userId, commentId);

        assertFalse(result);
    }

    @Test
    void canDeleteComment_CommentAlreadyDeleted_ReturnsFalse() {
        testComment.setIsDeleted(true);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);

        boolean result = accessService.canDeleteComment(userId, commentId);

        assertFalse(result);
    }

    @Test
    void canDeleteComment_PostNotFound_ReturnsFalse() {
        testComment.setOwnerId(new ObjectId());
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(null);

        boolean result = accessService.canDeleteComment(userId, commentId);

        assertFalse(result);
    }

    @Test
    void canDeleteComment_ForumNotFound_ReturnsFalse() {
        testComment.setOwnerId(new ObjectId());
        testPost.setOwnerId(new ObjectId());
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(mongoTemplate.findById(forumId, Forum.class)).thenReturn(null);

        boolean result = accessService.canDeleteComment(userId, commentId);

        assertFalse(result);
    }

    // ==================== VOTE DELETION TESTS ====================

    @Test
    void canDeleteVote_Owner_ReturnsTrue() {
        Document voteDoc = new Document()
                .append("userId", userId)
                .append("isDeleted", false);

        when(mongoTemplate.findOne(any(Query.class), eq(Document.class), eq("votes")))
                .thenReturn(voteDoc);

        boolean result = accessService.canDeleteVote(userId, voteId);

        assertTrue(result);
    }

    @Test
    void canDeleteVote_NotOwner_ReturnsFalse() {
        ObjectId differentUserId = new ObjectId();
        Document voteDoc = new Document()
                .append("userId", userId)
                .append("isDeleted", false);

        when(mongoTemplate.findOne(any(Query.class), eq(Document.class), eq("votes")))
                .thenReturn(voteDoc);

        boolean result = accessService.canDeleteVote(differentUserId, voteId);

        assertFalse(result);
    }

    @Test
    void canDeleteVote_VoteNotFound_ReturnsFalse() {
        when(mongoTemplate.findOne(any(Query.class), eq(Document.class), eq("votes")))
                .thenReturn(null);

        boolean result = accessService.canDeleteVote(userId, voteId);

        assertFalse(result);
    }

    @Test
    void canDeleteVote_VoteAlreadyDeleted_ReturnsFalse() {
        Document voteDoc = new Document()
                .append("userId", userId)
                .append("isDeleted", true);

        when(mongoTemplate.findOne(any(Query.class), eq(Document.class), eq("votes")))
                .thenReturn(voteDoc);

        boolean result = accessService.canDeleteVote(userId, voteId);

        assertFalse(result);
    }

    // ==================== OWNER ID GETTER TESTS ====================

    @Test
    void getForumOwnerId_ForumExists_ReturnsOwnerId() {
        when(mongoTemplate.findOne(any(Query.class), eq(Forum.class)))
                .thenReturn(testForum);

        ObjectId result = accessService.getForumOwnerId(forumId);

        assertEquals(userId, result);
    }

    @Test
    void getForumOwnerId_ForumNotFound_ReturnsNull() {
        when(mongoTemplate.findOne(any(Query.class), eq(Forum.class)))
                .thenReturn(null);

        ObjectId result = accessService.getForumOwnerId(forumId);

        assertNull(result);
    }

    @Test
    void getPostOwnerId_PostExists_ReturnsOwnerId() {
        when(mongoTemplate.findOne(any(Query.class), eq(Post.class)))
                .thenReturn(testPost);

        ObjectId result = accessService.getPostOwnerId(postId);

        assertEquals(userId, result);
    }

    @Test
    void getPostOwnerId_PostNotFound_ReturnsNull() {
        when(mongoTemplate.findOne(any(Query.class), eq(Post.class)))
                .thenReturn(null);

        ObjectId result = accessService.getPostOwnerId(postId);

        assertNull(result);
    }

    @Test
    void getCommentOwnerId_CommentExists_ReturnsOwnerId() {
        when(mongoTemplate.findOne(any(Query.class), eq(Comment.class)))
                .thenReturn(testComment);

        ObjectId result = accessService.getCommentOwnerId(commentId);

        assertEquals(userId, result);
    }

    @Test
    void getCommentOwnerId_CommentNotFound_ReturnsNull() {
        when(mongoTemplate.findOne(any(Query.class), eq(Comment.class)))
                .thenReturn(null);

        ObjectId result = accessService.getCommentOwnerId(commentId);

        assertNull(result);
    }
}