package org.example.backend.moderation;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the moderation topics; Spring's KafkaAdmin creates them at startup
 * (broker auto-create is off so partition counts stay deliberate). Partition count
 * is the worker fleet's max parallelism — 12 leaves scaling headroom without
 * repartitioning. replicas=1 matches the single-broker dev/compose deployment;
 * a production cluster would use RF=3 with min.insync.replicas=2.
 */
@Configuration
public class ModerationKafkaConfig {

    @Value("${moderation.topics.requests}")
    private String requestsTopic;

    @Value("${moderation.topics.verdicts}")
    private String verdictsTopic;

    @Value("${moderation.topics.dlq}")
    private String dlqTopic;

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
}
