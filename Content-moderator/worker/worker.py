"""Micro-batching Kafka worker: consume moderation requests, run ONNX inference
in batches, produce verdicts.

Offsets are committed only after every verdict in the batch is confirmed written
(at-least-once). On any unexpected failure the process exits and the container
restarts, so uncommitted messages are redelivered by the consumer group — the
backend applies verdicts idempotently, so redelivery is harmless.

Message contracts (JSON values, keyed by contentId so per-content order is
preserved through partitioning):

  moderation.requests:
    {"v":1, "contentType":"POST", "contentId":"...", "version":3, "text":"..."}

  moderation.verdicts:
    {"v":1, "contentType":"POST", "contentId":"...", "version":3,
     "flagged":true, "scores":{"toxic":0.98, "severe_toxic":0.02}}

Malformed requests are forwarded to the DLQ topic instead of wedging the
partition. Batching is natural here: one consume() call returns up to MAX_BATCH
messages, topped up within LINGER_MS — one inference call per batch.
"""
import json
import logging
import os

from confluent_kafka import Consumer, KafkaException, Producer

import inference

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("worker")

BOOTSTRAP_SERVERS = os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
REQUESTS_TOPIC = os.environ.get("REQUESTS_TOPIC", "moderation.requests")
VERDICTS_TOPIC = os.environ.get("VERDICTS_TOPIC", "moderation.verdicts")
DLQ_TOPIC = os.environ.get("DLQ_TOPIC", "moderation.requests.dlq")
GROUP_ID = os.environ.get("GROUP_ID", "moderation-workers")

MAX_BATCH = int(os.environ.get("MAX_BATCH", "32"))
LINGER_MS = int(os.environ.get("LINGER_MS", "15"))
IDLE_BLOCK_S = 1.0  # consume timeout while the buffer is empty

TOXIC_THRESHOLD = float(os.environ.get("TOXIC_THRESHOLD", "0.5"))
SEVERE_TOXIC_THRESHOLD = float(os.environ.get("SEVERE_TOXIC_THRESHOLD", "0.5"))


def build_consumer() -> Consumer:
    return Consumer(
        {
            "bootstrap.servers": BOOTSTRAP_SERVERS,
            "group.id": GROUP_ID,
            # Commit manually, only after verdicts are confirmed written.
            "enable.auto.commit": False,
            # First deployment / new group: start from the oldest retained
            # request so a backlog accumulated before the workers came up
            # still gets moderated.
            "auto.offset.reset": "earliest",
        }
    )


def build_producer() -> Producer:
    return Producer(
        {
            "bootstrap.servers": BOOTSTRAP_SERVERS,
            # Idempotent producer: implies acks=all and retry-safe writes, so a
            # verdict is never silently lost or duplicated by the producer itself.
            "enable.idempotence": True,
        }
    )


def parse_request(msg) -> dict | None:
    """Parsed request dict, or None if the message is malformed (→ DLQ)."""
    try:
        req = json.loads(msg.value())
        if not isinstance(req.get("text"), str) or not req.get("contentId"):
            raise ValueError("missing text/contentId")
        return req
    except (ValueError, TypeError) as e:  # json.JSONDecodeError subclasses ValueError
        log.warning(
            "malformed request %s[%d]@%d -> DLQ: %s", msg.topic(), msg.partition(), msg.offset(), e
        )
        return None


def flush(consumer: Consumer, producer: Producer, batch: list) -> None:
    """Score one micro-batch, produce verdicts (+ DLQ for malformed messages),
    then commit offsets. Raises on any delivery failure so the process dies
    without committing and the batch is redelivered."""
    delivery_errors: list = []

    def on_delivery(err, _msg):
        if err is not None:
            delivery_errors.append(err)

    valid: list[tuple] = []  # (msg, request)
    for msg in batch:
        req = parse_request(msg)
        if req is None:
            producer.produce(DLQ_TOPIC, value=msg.value(), key=msg.key(), on_delivery=on_delivery)
        else:
            valid.append((msg, req))

    if valid:
        scores = inference.score([req["text"] for _, req in valid])
        for (msg, req), s in zip(valid, scores):
            flagged = s["toxic"] >= TOXIC_THRESHOLD or s["severe_toxic"] >= SEVERE_TOXIC_THRESHOLD
            verdict = {
                "v": 1,
                "contentType": req.get("contentType"),
                "contentId": req["contentId"],
                "version": req.get("version"),
                "flagged": flagged,
                "scores": s,
            }
            producer.produce(
                VERDICTS_TOPIC,
                value=json.dumps(verdict),
                key=msg.key(),
                on_delivery=on_delivery,
            )

    producer.flush()
    if delivery_errors:
        raise KafkaException(delivery_errors[0])

    # Verdicts are durable in Kafka — now it is safe to move the group's offset.
    consumer.commit(asynchronous=False)
    log.info("flushed batch size=%d (valid=%d)", len(batch), len(valid))


def main() -> None:
    consumer = build_consumer()
    producer = build_producer()
    consumer.subscribe([REQUESTS_TOPIC])
    log.info(
        "worker ready: group=%s requests=%s verdicts=%s dlq=%s",
        GROUP_ID, REQUESTS_TOPIC, VERDICTS_TOPIC, DLQ_TOPIC,
    )

    try:
        while True:
            batch = consumer.consume(num_messages=MAX_BATCH, timeout=IDLE_BLOCK_S)
            if not batch:
                continue
            # Linger briefly to top up a partial batch — same latency/throughput
            # trade-off as the consume timeout, expressed in one place.
            if len(batch) < MAX_BATCH:
                batch += consumer.consume(
                    num_messages=MAX_BATCH - len(batch), timeout=LINGER_MS / 1000.0
                )

            errors = [m for m in batch if m.error() is not None]
            for m in errors:
                # Transport-level errors (not message payloads). Log and let the
                # consumer recover; the affected messages will be redelivered.
                log.warning("consumer error: %s", m.error())
            batch = [m for m in batch if m.error() is None]
            if batch:
                flush(consumer, producer, batch)
    finally:
        consumer.close()


if __name__ == "__main__":
    main()
