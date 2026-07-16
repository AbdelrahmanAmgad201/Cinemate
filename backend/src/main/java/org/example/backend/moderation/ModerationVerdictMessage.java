package org.example.backend.moderation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Wire format of a message on {@code moderation.verdicts}, produced by the worker.
 * {@code version} echoes the request's version untouched — the consumer uses it to
 * discard verdicts for text that was edited while the request was in flight.
 * Boxed types on purpose: a missing field parses as null and is rejected explicitly
 * rather than silently defaulting.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModerationVerdictMessage(
        String contentType,
        String contentId,
        Long version,
        Boolean flagged,
        Map<String, Double> scores) {
}
