package org.example.backend.moderation;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Declares the moderation topics; Spring's KafkaAdmin creates them at startup
 * (broker auto-create is off so partition counts stay deliberate). Partition count
 * is the worker fleet's max parallelism — 12 leaves scaling headroom without
 * repartitioning. replicas=1 matches the single-broker dev/compose deployment;
 * a production cluster would use RF=3 with min.insync.replicas=2.
 *
 * <p>Also wires the verdict listener's error handling (MOD-02): a record that keeps
 * throwing is retried a bounded number of times, then published to {@link #verdictsDlqTopic}
 * instead of being silently committed and dropped.
 */
@Configuration
public class ModerationKafkaConfig {

    @Value("${moderation.topics.requests}")
    private String requestsTopic;

    @Value("${moderation.topics.verdicts}")
    private String verdictsTopic;

    @Value("${moderation.topics.dlq}")
    private String dlqTopic;

    @Value("${moderation.topics.verdicts-dlq}")
    private String verdictsDlqTopic;

    @Value("${moderation.topics.partitions:12}")
    private int partitions;

    @Bean
    public NewTopic moderationRequestsTopic() {
        return TopicBuilder.name(requestsTopic).partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic moderationVerdictsTopic() {
        return TopicBuilder.name(verdictsTopic).partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic moderationDlqTopic() {
        // Poison messages only — no parallelism needed.
        return TopicBuilder.name(dlqTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic moderationVerdictsDlqTopic() {
        // Verdicts that exhaust their retry budget — no parallelism needed.
        return TopicBuilder.name(verdictsDlqTopic).partitions(1).replicas(1).build();
    }

    /**
     * Retries a throwing verdict 3 times (1s apart), then republishes the record — key,
     * value and all — to {@link #verdictsDlqTopic} and commits the offset, so a stuck
     * datastore or a bug can no longer silently lose a flagged verdict. Fixed to a single
     * destination regardless of source partition, matching the DLQ topic's single partition.
     */
    @Bean
    public DefaultErrorHandler moderationVerdictErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, exception) -> new TopicPartition(verdictsDlqTopic, -1));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    }

    /**
     * Same property-driven setup as Spring Boot's autoconfigured factory (ack-mode, group-id,
     * etc. from {@code spring.kafka.*}), plus the DLQ error handler above. Explicit so a future
     * second {@code @KafkaListener} elsewhere doesn't silently inherit verdict-specific DLQ
     * routing.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> moderationVerdictListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            DefaultErrorHandler moderationVerdictErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(moderationVerdictErrorHandler);
        return factory;
    }
}
