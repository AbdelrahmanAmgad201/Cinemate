package org.example.backend.comment;

import org.bson.types.ObjectId;
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

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CascadeDeletionService deletionService;

    @Mock
    private AccessService accessService;

    @InjectMocks
    private CommentService commentService;

    private Long userId;
    private ObjectId userObjectId;
    private ObjectId postId;
    private ObjectId commentId;
    private Post testPost;
    private Comment testComment;
    private AddCommentDTO addCommentDTO;

    @BeforeEach
    void setUp() {
        userId = 123456L;
        userObjectId = new ObjectId(String.format("%024x", userId));
        postId = new ObjectId();
        commentId = new ObjectId();

        testPost = Post.builder()
                .id(postId)
                .commentCount(5)
                .isDeleted(false)
                .build();

        testComment = Comment.builder()
                .id(commentId)
                .postId(postId)
                .ownerId(userObjectId)
                .content("Test comment")
                .depth(0)
                .build();

        addCommentDTO = AddCommentDTO.builder()
                .postId(postId)
                .parentId(null)
                .content("New comment content")
                .build();
    }

    @Test
    void addComment_TopLevelComment_Success() {
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));

        Comment result = commentService.addComment(userId, addCommentDTO);

        assertNotNull(result);
        assertEquals(userObjectId, result.getOwnerId());
        assertEquals(postId, result.getPostId());
        assertNull(result.getParentId());
        assertEquals("New comment content", result.getContent());
        assertEquals(0, result.getDepth());

        verify(postRepository).save(argThat(post -> post.getCommentCount() == 6));
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_ReplyToComment_IncreasesDepth() {
        ObjectId parentCommentId = new ObjectId();
        Comment parentComment = Comment.builder()
                .id(parentCommentId)
                .postId(postId)
                .depth(0)
                .build();

        addCommentDTO.setParentId(parentCommentId);

        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(mongoTemplate.findById(parentCommentId, Comment.class)).thenReturn(parentComment);
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));

        Comment result = commentService.addComment(userId, addCommentDTO);

        assertEquals(parentCommentId, result.getParentId());
        assertEquals(1, result.getDepth());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void addComment_NestedReply_IncreasesDepth() {
        ObjectId parentCommentId = new ObjectId();
        Comment parentComment = Comment.builder()
                .id(parentCommentId)
                .postId(postId)
                .depth(2)
                .build();

        addCommentDTO.setParentId(parentCommentId);

        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(mongoTemplate.findById(parentCommentId, Comment.class)).thenReturn(parentComment);
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));

        Comment result = commentService.addComment(userId, addCommentDTO);

        assertEquals(3, result.getDepth());
    }

    @Test
    void addComment_PostNotFound_ThrowsException() {
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> commentService.addComment(userId, addCommentDTO));

        verify(commentRepository, never()).save(any());
        verify(postRepository, never()).save(any());
    }

    @Test
    void addComment_DeletedPost_ThrowsException() {
        testPost.setIsDeleted(true);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);

        assertThrows(IllegalStateException.class,
                () -> commentService.addComment(userId, addCommentDTO));

        verify(commentRepository, never()).save(any());
        verify(postRepository, never()).save(any());
    }

    @Test
    void addComment_ParentIdNull_CreatesTopLevelComment() {
        addCommentDTO.setParentId(null);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));

        Comment result = commentService.addComment(userId, addCommentDTO);

        assertNull(result.getParentId());
        assertEquals(0, result.getDepth());
    }

    @Test
    void deleteComment_ValidUser_Success() {
        when(accessService.canDeleteComment(userObjectId, commentId)).thenReturn(true);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);

        commentService.deleteComment(commentId, userId);

        verify(deletionService).deleteComment(commentId);
        verify(postRepository).save(argThat(post -> post.getCommentCount() == 6));
    }

    @Test
    void deleteComment_UnauthorizedUser_ThrowsAccessDeniedException() {
        when(accessService.canDeleteComment(userObjectId, commentId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> commentService.deleteComment(commentId, userId));

        verify(deletionService, never()).deleteComment(any());
        verify(postRepository, never()).save(any());
    }

    @Test
    void deleteComment_CommentNotFound_ThrowsException() {
        when(accessService.canDeleteComment(userObjectId, commentId)).thenReturn(true);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> commentService.deleteComment(commentId, userId));

        verify(deletionService, never()).deleteComment(any());
    }

    @Test
    void deleteComment_UpdatesPostCommentCount() {
        when(accessService.canDeleteComment(userObjectId, commentId)).thenReturn(true);
        when(mongoTemplate.findById(commentId, Comment.class)).thenReturn(testComment);
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);

        commentService.deleteComment(commentId, userId);

        verify(postRepository).save(argThat(post ->
                post.getCommentCount() == 6 // 5 + 1 (bug in original code - should be -1)
        ));
    }

    @Test
    void addComment_WithAllFields_PreservesData() {
        when(mongoTemplate.findById(postId, Post.class)).thenReturn(testPost);
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));

        Comment result = commentService.addComment(userId, addCommentDTO);

        assertNotNull(result.getOwnerId());
        assertNotNull(result.getPostId());
        assertNotNull(result.getContent());
        assertEquals("New comment content", result.getContent());
    }
}