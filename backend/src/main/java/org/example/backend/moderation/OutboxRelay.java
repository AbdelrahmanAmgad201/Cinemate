package org.example.backend.moderation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publishes outbox entries to Kafka and deletes them only after the broker acks
 * (delete-after-ack ⇒ at-least-once; a crash between send and delete re-publishes
 * a duplicate, which the idempotent pipeline absorbs). Kafka being down just means
 * entries accumulate and get retried next tick — content creation is unaffected,
 * which is what makes the optimistic-publish model resilient.
 *
 * Note: assumes a single backend instance (as deployed in compose). Multiple
 * instances would need a leader lock (e.g. ShedLock) around this poller to avoid
 * duplicate publishes — harmless for correctness, wasteful at scale.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final ModerationOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${moderation.topics.requests}")
    private String requestsTopic;

    @Value("${moderation.outbox.batch-size:100}")
    private int batchSize;

    @Value("${moderation.outbox.send-timeout-s:10}")
    private long sendTimeoutSeconds;

    @Scheduled(
            fixedDelayString = "${moderation.outbox.relay-delay-ms:1000}",
            initialDelayString = "${moderation.outbox.relay-initial-delay-ms:1000}")
    public void relay() {
        List<ModerationOutboxEntry> batch =
                outboxRepository.findAllByOrderByIdAsc(PageRequest.of(0, batchSize));

        for (ModerationOutboxEntry entry : batch) {
            String payload;
            try {
                payload = objectMapper.writeValueAsString(ModerationRequestMessage.from(entry));
            } catch (JsonProcessingException e) {
                // Can never succeed on retry — drop it rather than wedge the relay.
                log.error("Unserializable outbox entry {} removed", entry.getId(), e);
                outboxRepository.deleteById(entry.getId());
                continue;
            }
            try {
                // Key by contentId: two edits of the same post land on the same
                // partition in order. Blocking send keeps delete-after-ack simple.
                kafkaTemplate.send(requestsTopic, entry.getContentId().toString(), payload)
                        .get(sendTimeoutSeconds, TimeUnit.SECONDS);
                outboxRepository.deleteById(entry.getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // Broker down/slow: stop here (preserves per-content order) and let
                // the next tick retry from the same entry.
                log.warn("Kafka publish failed, outbox will retry ({} entries pending): {}",
                        batch.size(), e.getMessage());
                return;
            }
        }
    }
}
