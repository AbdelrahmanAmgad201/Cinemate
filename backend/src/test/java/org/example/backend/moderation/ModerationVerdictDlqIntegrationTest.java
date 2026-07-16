package org.example.backend.moderation;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MOD-02: proves the container/error-handler wiring end to end against a real (embedded)
 * broker — a listener that keeps throwing is retried a bounded number of times, then the
 * record is republished to the verdicts DLQ with its key preserved, and the offset is
 * committed (no infinite redelivery loop). {@link ModerationVerdictConsumerTest} covers the
 * real consumer's business logic with mocks; this covers only the plumbing declared in
 * {@link ModerationKafkaConfig}, exercised through a throwing stand-in listener.
 */
@SpringJUnitConfig(classes = ModerationVerdictDlqIntegrationTest.TestConfig.class)
@EmbeddedKafka(partitions = 1, topics = "moderation.verdicts.dlq", brokerProperties = "auto.create.topics.enable=true")
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=mod02-dlq-test",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.enable-auto-commit=false",
        "spring.kafka.listener.ack-mode=record",
        "moderation.topics.requests=moderation.requests",
        "moderation.topics.verdicts=moderation.verdicts",
        "moderation.topics.dlq=moderation.requests.dlq",
        "moderation.topics.verdicts-dlq=moderation.verdicts.dlq",
})
class ModerationVerdictDlqIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private EmbeddedKafkaBroker broker;
    @Autowired
    private PoisonListener poisonListener;

    @Test
    void recordThatAlwaysThrows_isRetriedThenSentToDlq() {
        String key = "content-123";
        kafkaTemplate.send("moderation.verdicts", key, "poison-payload");

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(broker, "dlq-verifier", true);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);

        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, "moderation.verdicts.dlq");
            ConsumerRecord<String, String> dlqRecord =
                    KafkaTestUtils.getSingleRecord(consumer, "moderation.verdicts.dlq", Duration.ofSeconds(15));
            assertEquals(key, dlqRecord.key());
            assertEquals("poison-payload", dlqRecord.value());
        }

        // 1 initial attempt + 3 retries (FixedBackOff(1000, 3) in ModerationKafkaConfig) before
        // the recoverer runs — proves the bounded backoff actually ran, not an immediate skip.
        assertEquals(4, poisonListener.attempts.get());
    }

    @Configuration
    @Import({KafkaAutoConfiguration.class, ModerationKafkaConfig.class})
    static class TestConfig {
        @Bean
        PoisonListener poisonListener() {
            return new PoisonListener();
        }
    }

    static class PoisonListener {
        final AtomicInteger attempts = new AtomicInteger();

        @KafkaListener(topics = "${moderation.topics.verdicts}", containerFactory = "moderationVerdictListenerContainerFactory")
        void onMessage(String payload) {
            attempts.incrementAndGet();
            throw new RuntimeException("always fails: " + payload);
        }
    }
}
