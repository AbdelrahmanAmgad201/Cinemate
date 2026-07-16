package org.example.backend.moderation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MOD-01: closes the outbox's missing half. The outbox guarantees a moderation request is
 * *published*, but nothing previously re-checked content whose verdict never came back (a
 * dropped message, a worker bug, a consumer failure past its retry budget). This re-enqueues
 * PENDING content whose current moderation request is older than the configured threshold.
 *
 * <p>Re-enqueuing writes a fresh outbox row for the SAME moderationVersion — it does not
 * touch the content itself, so a genuinely in-flight verdict (just slow) is still
 * authoritative when it eventually arrives; a duplicate request is harmless (the pipeline is
 * idempotent by design, see moderation-architecture.md §5.3). {@code moderationRequestedAt}
 * is bumped to "now" for every swept row before enqueuing, so the same row isn't re-swept
 * again next tick before the new request has had a chance to be answered.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationReconciliationService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ForumRepository forumRepository;
    private final ModerationOutboxService moderationOutboxService;

    @Transactional
    public int sweepPendingPosts(Instant cutoff, int batchSize) {
        List<Post> stuck = postRepository.findByModerationStatusAndModerationRequestedAtBefore(
                ModerationStatus.PENDING, cutoff, batchPage(batchSize));
        if (stuck.isEmpty()) {
            return 0;
        }
        postRepository.touchModerationRequestedAt(idsOf(stuck, Post::getId), Instant.now());
        for (Post post : stuck) {
            moderationOutboxService.enqueue(ContentType.POST, post.getId(),
                    post.getModerationVersion(), post.getTitle() + "\n" + post.getContent());
        }
        log.info("Reconciliation sweep re-enqueued {} stuck PENDING post(s)", stuck.size());
        return stuck.size();
    }

    @Transactional
    public int sweepPendingComments(Instant cutoff, int batchSize) {
        List<Comment> stuck = commentRepository.findByModerationStatusAndModerationRequestedAtBefore(
                ModerationStatus.PENDING, cutoff, batchPage(batchSize));
        if (stuck.isEmpty()) {
            return 0;
        }
        commentRepository.touchModerationRequestedAt(idsOf(stuck, Comment::getId), Instant.now());
        for (Comment comment : stuck) {
            moderationOutboxService.enqueue(ContentType.COMMENT, comment.getId(),
                    comment.getModerationVersion(), comment.getContent());
        }
        log.info("Reconciliation sweep re-enqueued {} stuck PENDING comment(s)", stuck.size());
        return stuck.size();
    }

    @Transactional
    public int sweepPendingForums(Instant cutoff, int batchSize) {
        List<Forum> stuck = forumRepository.findByModerationStatusAndModerationRequestedAtBefore(
                ModerationStatus.PENDING, cutoff, batchPage(batchSize));
        if (stuck.isEmpty()) {
            return 0;
        }
        forumRepository.touchModerationRequestedAt(idsOf(stuck, Forum::getId), Instant.now());
        for (Forum forum : stuck) {
            moderationOutboxService.enqueue(ContentType.FORUM, forum.getId(),
                    forum.getModerationVersion(), forum.getName() + "\n" + forum.getDescription());
        }
        log.info("Reconciliation sweep re-enqueued {} stuck PENDING forum(s)", stuck.size());
        return stuck.size();
    }

    private static Pageable batchPage(int batchSize) {
        return PageRequest.of(0, batchSize);
    }

    private static <T> List<UUID> idsOf(List<T> items, java.util.function.Function<T, UUID> idFn) {
        return items.stream().map(idFn).collect(Collectors.toList());
    }
}
