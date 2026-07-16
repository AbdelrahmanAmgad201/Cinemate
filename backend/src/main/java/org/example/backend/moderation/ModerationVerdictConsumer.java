package org.example.backend.moderation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.comment.CommentService;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.example.backend.forum.ForumService;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.example.backend.post.PostService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Applies moderation verdicts (optimistic-publish model: a flagged verdict retroactively
 * soft-deletes already-visible content). Idempotent (at-least-once delivery):
 * <ul>
 *   <li>already-deleted content → no-op</li>
 *   <li>version mismatch → no-op (text was edited while this verdict was in flight)</li>
 *   <li>clean verdicts only flip PENDING→APPROVED for the matching version</li>
 * </ul>
 * Verdicts for one content id share a Kafka partition (keyed by contentId), so they arrive
 * ordered and are applied single-threaded — the in-code version guard is sufficient (no
 * concurrent verdict application for the same content).
 *
 * <p>MOD-02: an exception thrown here (e.g. a datastore blip) is retried by
 * {@code moderationVerdictListenerContainerFactory}'s {@link org.springframework.kafka.listener.DefaultErrorHandler}
 * a bounded number of times, then the record is published to the verdicts DLQ instead of
 * being silently committed and lost — see {@link ModerationKafkaConfig}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationVerdictConsumer {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ForumRepository forumRepository;
    private final PostService postService;
    private final CommentService commentService;
    private final ForumService forumService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${moderation.topics.verdicts}", containerFactory = "moderationVerdictListenerContainerFactory")
    @Transactional
    public void onVerdict(String payload) {
        ModerationVerdictMessage verdict;
        try {
            verdict = objectMapper.readValue(payload, ModerationVerdictMessage.class);
        } catch (JsonProcessingException e) {
            log.error("Unparseable moderation verdict skipped: {}", payload, e);
            return;
        }
        if (verdict.contentType() == null || verdict.contentId() == null
                || verdict.version() == null || verdict.flagged() == null) {
            log.error("Incomplete moderation verdict skipped: {}", payload);
            return;
        }

        ContentType contentType;
        UUID contentId;
        try {
            contentType = ContentType.valueOf(verdict.contentType());
            contentId = UUID.fromString(verdict.contentId());
        } catch (IllegalArgumentException e) {
            log.error("Moderation verdict with invalid type/id skipped: {}", payload);
            return;
        }

        switch (contentType) {
            case POST -> apply(verdict, contentId, postRepository,
                    postRepository::approveModeration, postRepository::markModerationRemoved,
                    postService::systemDeletePost);
            case COMMENT -> apply(verdict, contentId, commentRepository,
                    commentRepository::approveModeration, commentRepository::markModerationRemoved,
                    commentService::systemDeleteComment);
            case FORUM -> apply(verdict, contentId, forumRepository,
                    forumRepository::approveModeration, forumRepository::markModerationRemoved,
                    forum -> forumService.systemDeleteForum(forum.getId()));
        }
    }


    private <T extends Moderatable> void apply(
            ModerationVerdictMessage verdict, UUID contentId, JpaRepository<T, UUID> repository,
            BiConsumer<UUID, Long> approve, Consumer<UUID> markRemoved, Consumer<T> remove) {

        if (!verdict.flagged()) {
            // Version+status guarded in SQL: a stale clean verdict can't approve text it never
            // saw, and redelivery is a no-op once APPROVED.
            approve.accept(contentId, verdict.version());
            return;
        }

        T content = repository.findById(contentId).orElse(null); // read-only guard
        if (content == null || Boolean.TRUE.equals(content.getIsDeleted())) {
            return; // already gone — idempotent no-op
        }
        if (content.getModerationVersion() != verdict.version()) {
            log.info("Stale flagged verdict ignored for {} (verdict v{}, current v{})",
                    contentId, verdict.version(), content.getModerationVersion());
            return;
        }

        markRemoved.accept(contentId); // bulk: moderation_status = REMOVED (audit trail)
        remove.accept(content);        // bulk: soft-delete cascade
        log.info("Moderation removed {} (toxic scores: {})", contentId, verdict.scores());
    }
}
