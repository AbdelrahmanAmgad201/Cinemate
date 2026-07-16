package org.example.backend.moderation;

import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationReconciliationServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ForumRepository forumRepository;
    @Mock
    private ModerationOutboxService moderationOutboxService;

    @InjectMocks
    private ModerationReconciliationService reconciliationService;

    private final Instant cutoff = Instant.parse("2026-07-16T00:00:00Z");

    // -----------------------------------------------------
    // TEST: sweepPendingPosts()
    // -----------------------------------------------------
    @Test
    void sweepPendingPosts_noStuckPosts_doesNothing() {
        when(postRepository.findByModerationStatusAndModerationRequestedAtBefore(
                eq(ModerationStatus.PENDING), eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of());

        int swept = reconciliationService.sweepPendingPosts(cutoff, 100);

        assertEquals(0, swept);
        verify(postRepository, never()).touchModerationRequestedAt(any(), any());
        verifyNoInteractions(moderationOutboxService);
    }

    @Test
    void sweepPendingPosts_reEnqueuesStuckPostsAndTouchesTimestamp() {
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .title("Title")
                .content("Body")
                .moderationVersion(2)
                .build();
        when(postRepository.findByModerationStatusAndModerationRequestedAtBefore(
                eq(ModerationStatus.PENDING), eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(post));

        int swept = reconciliationService.sweepPendingPosts(cutoff, 100);

        assertEquals(1, swept);
        ArgumentCaptor<List<UUID>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(postRepository).touchModerationRequestedAt(idsCaptor.capture(), any(Instant.class));
        assertEquals(List.of(post.getId()), idsCaptor.getValue());
        verify(moderationOutboxService).enqueue(
                ContentType.POST, post.getId(), 2, "Title\nBody");
    }

    // -----------------------------------------------------
    // TEST: sweepPendingComments()
    // -----------------------------------------------------
    @Test
    void sweepPendingComments_reEnqueuesStuckComments() {
        Comment comment = Comment.builder()
                .id(UUID.randomUUID())
                .content("hello world")
                .moderationVersion(1)
                .build();
        when(commentRepository.findByModerationStatusAndModerationRequestedAtBefore(
                eq(ModerationStatus.PENDING), eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(comment));

        int swept = reconciliationService.sweepPendingComments(cutoff, 100);

        assertEquals(1, swept);
        verify(commentRepository).touchModerationRequestedAt(eq(List.of(comment.getId())), any(Instant.class));
        verify(moderationOutboxService).enqueue(
                ContentType.COMMENT, comment.getId(), 1, "hello world");
    }

    // -----------------------------------------------------
    // TEST: sweepPendingForums()
    // -----------------------------------------------------
    @Test
    void sweepPendingForums_reEnqueuesStuckForums() {
        Forum forum = Forum.builder()
                .id(UUID.randomUUID())
                .name("Forum")
                .description("Description")
                .moderationVersion(3)
                .build();
        when(forumRepository.findByModerationStatusAndModerationRequestedAtBefore(
                eq(ModerationStatus.PENDING), eq(cutoff), any(Pageable.class)))
                .thenReturn(List.of(forum));

        int swept = reconciliationService.sweepPendingForums(cutoff, 100);

        assertEquals(1, swept);
        verify(forumRepository).touchModerationRequestedAt(eq(List.of(forum.getId())), any(Instant.class));
        verify(moderationOutboxService).enqueue(
                ContentType.FORUM, forum.getId(), 3, "Forum\nDescription");
    }
}
