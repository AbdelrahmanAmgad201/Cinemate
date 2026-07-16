package org.example.backend.moderation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.comment.CommentService;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.example.backend.forum.ForumService;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.example.backend.post.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Covers the verdict-application business logic, plus the precondition MOD-02's DLQ
 * relies on: an unexpected failure must propagate out of {@code onVerdict} rather than
 * being swallowed, so the container's error handler actually gets a chance to retry and
 * dead-letter it (see {@link ModerationKafkaConfig}).
 */
@ExtendWith(MockitoExtension.class)
class ModerationVerdictConsumerTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ForumRepository forumRepository;
    @Mock
    private PostService postService;
    @Mock
    private CommentService commentService;
    @Mock
    private ForumService forumService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ModerationVerdictConsumer consumer;

    private ModerationVerdictConsumer newConsumer() {
        return new ModerationVerdictConsumer(postRepository, commentRepository, forumRepository,
                postService, commentService, forumService, objectMapper);
    }

    private String payload(String contentType, UUID contentId, long version, boolean flagged) {
        return """
                {"contentType":"%s","contentId":"%s","version":%d,"flagged":%b,"scores":{"toxic":0.9}}
                """.formatted(contentType, contentId, version, flagged);
    }

    // -----------------------------------------------------
    // Malformed input — logged and dropped, never thrown (would wedge the partition forever).
    // -----------------------------------------------------

    @Test
    void unparseableJson_isSkippedSilently() {
        consumer = newConsumer();
        assertDoesNotThrow(() -> consumer.onVerdict("not json"));
        verifyNoInteractions(postRepository, commentRepository, forumRepository);
    }

    @Test
    void missingRequiredField_isSkippedSilently() {
        consumer = newConsumer();
        String payload = "{\"contentType\":\"POST\",\"contentId\":\"" + UUID.randomUUID() + "\"}"; // no version/flagged
        assertDoesNotThrow(() -> consumer.onVerdict(payload));
        verifyNoInteractions(postRepository, commentRepository, forumRepository);
    }

    @Test
    void invalidContentTypeOrId_isSkippedSilently() {
        consumer = newConsumer();
        String payload = payload("NOT_A_TYPE", UUID.randomUUID(), 1, true);
        assertDoesNotThrow(() -> consumer.onVerdict(payload));
        verifyNoInteractions(postRepository, commentRepository, forumRepository);
    }

    // -----------------------------------------------------
    // Clean verdict — flips PENDING -> APPROVED for the matching version only.
    // -----------------------------------------------------

    @Test
    void cleanVerdict_approvesMatchingVersion() {
        consumer = newConsumer();
        UUID postId = UUID.randomUUID();

        consumer.onVerdict(payload("POST", postId, 2, false));

        verify(postRepository).approveModeration(postId, 2L);
        verifyNoInteractions(postService);
    }

    // -----------------------------------------------------
    // Flagged verdict — removes live content, idempotent no-op otherwise.
    // -----------------------------------------------------

    @Test
    void flaggedVerdict_removesLiveMatchingVersion() {
        consumer = newConsumer();
        UUID postId = UUID.randomUUID();
        Post post = Post.builder().id(postId).isDeleted(false).moderationVersion(3).build();
        when(postRepository.findById(postId)).thenReturn(java.util.Optional.of(post));

        consumer.onVerdict(payload("POST", postId, 3, true));

        verify(postRepository).markModerationRemoved(postId);
        verify(postService).systemDeletePost(post);
    }

    @Test
    void flaggedVerdict_alreadyDeleted_isNoOp() {
        consumer = newConsumer();
        UUID postId = UUID.randomUUID();
        Post post = Post.builder().id(postId).isDeleted(true).moderationVersion(3).build();
        when(postRepository.findById(postId)).thenReturn(java.util.Optional.of(post));

        consumer.onVerdict(payload("POST", postId, 3, true));

        verify(postRepository, never()).markModerationRemoved(any());
        verifyNoInteractions(postService);
    }

    @Test
    void flaggedVerdict_staleVersion_isNoOp() {
        consumer = newConsumer();
        UUID postId = UUID.randomUUID();
        Post post = Post.builder().id(postId).isDeleted(false).moderationVersion(5).build();
        when(postRepository.findById(postId)).thenReturn(java.util.Optional.of(post));

        consumer.onVerdict(payload("POST", postId, 3, true)); // verdict for an older version

        verify(postRepository, never()).markModerationRemoved(any());
        verifyNoInteractions(postService);
    }

    @Test
    void flaggedVerdict_contentAlreadyGone_isNoOp() {
        consumer = newConsumer();
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(java.util.Optional.empty());

        consumer.onVerdict(payload("POST", postId, 1, true));

        verify(postRepository, never()).markModerationRemoved(any());
        verifyNoInteractions(postService);
    }

    // -----------------------------------------------------
    // MOD-02 precondition: a real failure must escape onVerdict, not be caught.
    // -----------------------------------------------------

    @Test
    void repositoryFailureOnCleanVerdict_propagatesOutOfListener() {
        consumer = newConsumer();
        UUID postId = UUID.randomUUID();
        doThrow(new RuntimeException("datastore blip"))
                .when(postRepository).approveModeration(eq(postId), anyLong());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> consumer.onVerdict(payload("POST", postId, 1, false)));
        assertEquals("datastore blip", ex.getMessage());
    }

    @Test
    void repositoryFailureOnFlaggedVerdict_propagatesOutOfListener() {
        consumer = newConsumer();
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId))
                .thenThrow(new RuntimeException("datastore blip"));

        assertThrows(RuntimeException.class,
                () -> consumer.onVerdict(payload("POST", postId, 1, true)));
    }

    // -----------------------------------------------------
    // COMMENT / FORUM routing sanity (each content type wires to its own repo/service).
    // -----------------------------------------------------

    @Test
    void commentContentType_routesToCommentRepositoryAndService() {
        consumer = newConsumer();
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder().id(commentId).isDeleted(false).moderationVersion(1).build();
        when(commentRepository.findById(commentId)).thenReturn(java.util.Optional.of(comment));

        consumer.onVerdict(payload("COMMENT", commentId, 1, true));

        verify(commentRepository).markModerationRemoved(commentId);
        verify(commentService).systemDeleteComment(comment);
    }

    @Test
    void forumContentType_routesToForumRepositoryAndService() {
        consumer = newConsumer();
        UUID forumId = UUID.randomUUID();
        Forum forum = Forum.builder().id(forumId).isDeleted(false).moderationVersion(1).build();
        when(forumRepository.findById(forumId)).thenReturn(java.util.Optional.of(forum));

        consumer.onVerdict(payload("FORUM", forumId, 1, true));

        verify(forumRepository).markModerationRemoved(forumId);
        verify(forumService).systemDeleteForum(forumId);
    }
}
