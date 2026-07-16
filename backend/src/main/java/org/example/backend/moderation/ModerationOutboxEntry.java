package org.example.backend.moderation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional-outbox row for the moderation pipeline: inserted in the SAME database
 * transaction as the content row it describes, so "content saved" and "moderation
 * request recorded" cannot diverge. Now an ordinary JPA entity in the one Postgres
 * database — the second (Mongo) transaction manager and the replica-set requirement
 * that this pattern used to need are gone. {@link OutboxRelay} publishes entries to
 * Kafka and deletes them only after the broker acknowledges (at-least-once).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "moderation_outbox")
public class ModerationOutboxEntry {

    /** BIGINT identity is monotonic, so relaying in id order preserves per-content edit order. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "content_version", nullable = false)
    private long contentVersion;

    /** Snapshot of the text to moderate, taken at write time. */
    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
