package com.crypto.alerting.ingestion.service;

import com.crypto.alerting.commons.PriceEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinanceWebSocketClient {

    private final PriceProducer priceProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BINANCE_URI = "wss://stream.binance.com:9443/ws/btcusdt@trade";

    @PostConstruct
    public void connect() {
        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
        client.execute(URI.create(BINANCE_URI), session -> session.receive()
                .map(msg -> msg.getPayloadAsText())
                .flatMap(this::processMessage)
                .then())
                .subscribe(
                        null,
                        error -> log.error("Error in WebSocket connection", error),
                        () -> log.info("WebSocket connection closed"));
    }

    private Mono<Void> processMessage(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String symbol = node.get("s").asText();
            Double price = node.get("p").asDouble();
            Long timestamp = node.get("T").asLong();

            PriceEvent event = new PriceEvent(symbol, price, timestamp);
            return priceProducer.send(event);
        } catch (Exception e) {
            log.error("Failed to parse message: {}", json, e);
            return Mono.empty();
        }
    }
}
