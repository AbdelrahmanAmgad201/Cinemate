package org.example.backend.moderation;

/**
 * Wire format of a message on {@code moderation.requests}, keyed by contentId so
 * per-content ordering survives partitioning. {@code text} is the write-time
 * snapshot from the outbox — the worker never reads our database.
 */
public record ModerationRequestMessage(
        int v,
        String contentType,
        String contentId,
        long version,
        String text) {

    public static final int SCHEMA_VERSION = 1;

    public static ModerationRequestMessage from(ModerationOutboxEntry entry) {
        return new ModerationRequestMessage(
                SCHEMA_VERSION,
                entry.getContentType(),
                entry.getContentId().toString(),
                entry.getContentVersion(),
                entry.getText());
    }
}
