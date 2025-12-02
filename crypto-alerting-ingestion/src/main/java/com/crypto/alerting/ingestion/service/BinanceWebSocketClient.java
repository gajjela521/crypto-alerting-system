package com.crypto.alerting.ingestion.service;

import com.crypto.alerting.commons.PriceEvent;
import com.crypto.alerting.ingestion.client.WebSocketClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Service responsible for managing WebSocket connection to Binance API.
 * Implements resilience patterns (Circuit Breaker, Retry, Rate Limiting) for
 * robust connection handling.
 * Processes incoming price data and broadcasts to downstream consumers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class BinanceWebSocketClient {

    // Dependencies
    private final PriceProducer priceProducer;
    private final PriceStreamService priceStreamService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final WebSocketClient webSocketClient;

    // Constants
    private static final String BINANCE_URI = "wss://stream.binance.com:9443/ws/btcusdt@trade";
    private static final String SERVICE_NAME = "binance-ws";
    private static final String FIELD_SYMBOL = "s";
    private static final String FIELD_PRICE = "p";
    private static final String FIELD_TIMESTAMP = "T";

    // Immutable ObjectMapper instance
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes WebSocket connection on application startup.
     * Connection is managed with resilience patterns.
     */
    @PostConstruct
    public void connect() {
        connectWithResilience()
                .subscribe(
                        null,
                        error -> log.error("Error in WebSocket connection after resilience patterns", error),
                        () -> log.info("WebSocket connection closed"));
    }

    /**
     * Establishes WebSocket connection with resilience patterns applied.
     * Package-private for testing purposes.
     *
     * @return Mono that completes when connection is closed
     */
    Mono<Void> connectWithResilience() {
        return Mono.defer(() -> {
            log.info("Attempting to connect to Binance WebSocket...");
            return webSocketClient.execute(URI.create(BINANCE_URI), session -> {
                log.info("Connected to Binance WebSocket session: {}", session.getId());
                return session.receive()
                        .doOnNext(msg -> log.trace("Received message payload"))
                        .map(msg -> msg.getPayloadAsText())
                        .flatMap(this::processMessage)
                        .doOnError(e -> log.error("Error in WebSocket session", e))
                        .doOnTerminate(() -> log.info("WebSocket session terminated"))
                        .then();
            });
        })
                .doOnError(e -> log.error("Failed to establish WebSocket connection", e))
                .transformDeferred(io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
                        .of(circuitBreakerRegistry.circuitBreaker(SERVICE_NAME)))
                .transformDeferred(io.github.resilience4j.reactor.retry.RetryOperator
                        .of(retryRegistry.retry(SERVICE_NAME)))
                .transformDeferred(io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator
                        .of(rateLimiterRegistry.rateLimiter(SERVICE_NAME)));
    }

    /**
     * Processes incoming WebSocket message containing price data.
     * Validates message structure and content before broadcasting.
     *
     * @param json the raw JSON message
     * @return Mono that completes when processing is done
     */
    private Mono<Void> processMessage(final String json) {
        try {
            if (!isValidMessage(json)) {
                return Mono.empty();
            }

            final JsonNode node = objectMapper.readTree(json);

            if (!hasRequiredFields(node)) {
                log.warn("Message missing required fields: {}", json);
                return Mono.empty();
            }

            final String symbol = node.get(FIELD_SYMBOL).asText();
            final Double price = node.get(FIELD_PRICE).asDouble();
            final Long timestamp = node.get(FIELD_TIMESTAMP).asLong();

            if (!isValidPriceData(symbol, price)) {
                log.warn("Invalid price data - symbol: {}, price: {}", symbol, price);
                return Mono.empty();
            }

            final PriceEvent event = new PriceEvent(symbol, price, timestamp);
            log.debug("Processed price event: {} @ ${}", symbol, price);

            // Broadcast to UI stream
            priceStreamService.broadcastPrice(event);

            // Send to Kafka with error handling
            return sendToKafka(event);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("JSON parsing error for message: {}", json, e);
            return Mono.empty();
        } catch (Exception e) {
            log.error("Unexpected error processing message: {}", json, e);
            return Mono.empty();
        }
    }

    /**
     * Validates that the message is not null or empty.
     *
     * @param json the message to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidMessage(final String json) {
        if (json == null || json.trim().isEmpty()) {
            log.warn("Received null or empty message");
            return false;
        }
        return true;
    }

    /**
     * Checks if the JSON node contains all required fields.
     *
     * @param node the JSON node to check
     * @return true if all required fields are present
     */
    private boolean hasRequiredFields(final JsonNode node) {
        return node.has(FIELD_SYMBOL) && node.has(FIELD_PRICE) && node.has(FIELD_TIMESTAMP);
    }

    /**
     * Validates price data values.
     *
     * @param symbol the ticker symbol
     * @param price  the price value
     * @return true if data is valid
     */
    private boolean isValidPriceData(final String symbol, final Double price) {
        if (symbol == null || symbol.isEmpty()) {
            return false;
        }
        return price != null && price > 0;
    }

    /**
     * Sends price event to Kafka with error handling.
     *
     * @param event the price event to send
     * @return Mono that completes when send is done
     */
    private Mono<Void> sendToKafka(final PriceEvent event) {
        return priceProducer.send(event)
                .doOnError(e -> log.error("Failed to send price event to Kafka: {}", event, e))
                .onErrorResume(e -> {
                    log.error("Kafka send error, continuing stream", e);
                    return Mono.empty();
                });
    }
}
