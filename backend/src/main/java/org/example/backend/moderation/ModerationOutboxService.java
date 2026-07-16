package org.example.backend.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Records "this text needs moderating" as an outbox row. MUST be called inside the same
 * transaction as the content save (the caller owns the {@code @Transactional} boundary —
 * see PostService/CommentService/ForumService), so content + outbox commit atomically.
 * That's now an ordinary single-datasource transaction — no Mongo transaction template.
 */
@Service
@RequiredArgsConstructor
public class ModerationOutboxService {

    private final ModerationOutboxRepository outboxRepository;

    public void enqueue(ContentType contentType, UUID contentId, long version, String text) {
        outboxRepository.save(ModerationOutboxEntry.builder()
                .contentType(contentType.name())
                .contentId(contentId)
                .contentVersion(version)
                .text(text)
                .createdAt(Instant.now())
                .build());
    }
}
