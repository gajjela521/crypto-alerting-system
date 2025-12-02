package com.crypto.alerting.ingestion.service;

import com.crypto.alerting.commons.PriceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

/**
 * Service responsible for publishing price events to Kafka.
 * Handles asynchronous message production with error handling and logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class PriceProducer {

    private final KafkaSender<String, PriceEvent> kafkaSender;

    private static final String TOPIC = "topic-raw-prices";

    /**
     * Sends a price event to the Kafka topic.
     * Uses the ticker symbol as the message key for partitioning.
     *
     * @param event the price event to send
     * @return Mono that completes when the message is sent
     */
    public Mono<Void> send(final PriceEvent event) {
        try {
            if (event == null) {
                log.warn("Attempted to send null price event");
                return Mono.empty();
            }

            final String key = event.getTicker();
            final ProducerRecord<String, PriceEvent> record = new ProducerRecord<>(TOPIC, key, event);
            final SenderRecord<String, PriceEvent, String> senderRecord = SenderRecord.create(record, key);

            return kafkaSender
                    .send(Mono.just(senderRecord))
                    .doOnNext(r -> log.debug("Sent price event: {}", event))
                    .doOnError(e -> log.error("Failed to send price event: {}", event, e))
                    .then();
        } catch (final Exception e) {
            log.error("Error creating Kafka sender record for event: {}", event, e);
            return Mono.error(e);
        }
    }
}
