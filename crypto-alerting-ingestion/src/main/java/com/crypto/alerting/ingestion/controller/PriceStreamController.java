package com.crypto.alerting.ingestion.controller;

import com.crypto.alerting.commons.PriceEvent;
import com.crypto.alerting.ingestion.service.PriceStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * REST controller for exposing price data endpoints.
 * Provides Server-Sent Events stream for real-time price updates
 * and snapshot endpoint for latest prices.
 */
@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public final class PriceStreamController {

    private final PriceStreamService priceStreamService;

    private static final Duration SSE_TIMEOUT = Duration.ofMinutes(30);

    /**
     * Streams price events to clients using Server-Sent Events.
     * Automatically reconnects on error.
     *
     * @return Flux of Server-Sent Events containing price data
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PriceEvent>> streamPrices() {
        log.info("Client connected to price stream");

        return priceStreamService.getPriceStream()
                .map(this::createServerSentEvent)
                .timeout(SSE_TIMEOUT)
                .doOnError(e -> log.error("Error in SSE stream", e))
                .doOnCancel(() -> log.info("Client disconnected from price stream"))
                .onErrorResume(e -> {
                    log.error("SSE stream error, terminating connection", e);
                    return Flux.empty();
                });
    }

    /**
     * Returns a snapshot of the latest prices for all symbols.
     *
     * @return map of ticker symbols to their latest price events
     */
    @GetMapping("/latest")
    public Map<String, PriceEvent> getLatestPrices() {
        try {
            return priceStreamService.getLatestPrices();
        } catch (final Exception e) {
            log.error("Error retrieving latest prices", e);
            return Map.of();
        }
    }

    /**
     * Creates a Server-Sent Event from a price event.
     *
     * @param event the price event
     * @return ServerSentEvent wrapper
     */
    private ServerSentEvent<PriceEvent> createServerSentEvent(final PriceEvent event) {
        return ServerSentEvent.<PriceEvent>builder()
                .id(String.valueOf(event.getTimestamp()))
                .event("price-update")
                .data(event)
                .build();
    }
}
