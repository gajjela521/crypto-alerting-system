package com.crypto.alerting.ingestion.service;

import com.crypto.alerting.commons.PriceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceProducer {

    private final KafkaSender<String, PriceEvent> kafkaSender;
    private static final String TOPIC = "topic-raw-prices";

    public Mono<Void> send(PriceEvent event) {
        return kafkaSender
                .send(Mono.just(
                        SenderRecord.create(new ProducerRecord<>(TOPIC, event.getTicker(), event), event.getTicker())))
                .doOnNext(r -> log.debug("Sent price event: {}", event))
                .then();
    }
}
