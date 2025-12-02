package com.crypto.alerting.ingestion.service;

import com.crypto.alerting.commons.PriceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing real-time price event streaming to UI clients.
 * Maintains a broadcast sink for Server-Sent Events and caches latest prices.
 */
@Service
@Slf4j
public final class PriceStreamService {

    // Multicast sink for broadcasting price events to multiple subscribers
    private final Sinks.Many<PriceEvent> priceSink = Sinks.many().multicast().onBackpressureBuffer();

    // Thread-safe cache of latest prices by ticker symbol
    private final Map<String, PriceEvent> latestPrices = new ConcurrentHashMap<>();

    /**
     * Broadcasts a price event to all active subscribers.
     * Also updates the latest price cache.
     *
     * @param event the price event to broadcast
     */
    public void broadcastPrice(final PriceEvent event) {
        try {
            if (event == null) {
                log.warn("Attempted to broadcast null price event");
                return;
            }

            latestPrices.put(event.getTicker(), event);
            final Sinks.EmitResult result = priceSink.tryEmitNext(event);

            if (result.isFailure()) {
                log.warn("Failed to emit price event for {}: {}", event.getTicker(), result);
            } else {
                log.debug("Broadcasted price event: {}", event);
            }
        } catch (final Exception e) {
            log.error("Error broadcasting price event", e);
        }
    }

    /**
     * Returns a Flux stream of price events for subscribers.
     * Includes lifecycle logging for monitoring.
     *
     * @return Flux of price events
     */
    public Flux<PriceEvent> getPriceStream() {
        return priceSink.asFlux()
                .doOnSubscribe(s -> log.info("New subscriber to price stream"))
                .doOnCancel(() -> log.info("Subscriber cancelled price stream"))
                .doOnError(e -> log.error("Error in price stream", e));
    }

    /**
     * Returns an immutable copy of the latest prices cache.
     *
     * @return map of ticker symbols to their latest price events
     */
    public Map<String, PriceEvent> getLatestPrices() {
        return Map.copyOf(latestPrices);
    }
}
