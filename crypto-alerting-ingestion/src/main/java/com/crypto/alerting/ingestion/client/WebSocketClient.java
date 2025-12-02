package com.crypto.alerting.ingestion.client;

import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Abstraction for WebSocket client operations.
 * Provides a clean interface for WebSocket connections.
 */
public interface WebSocketClient {

    /**
     * Executes a WebSocket connection to the specified URI.
     *
     * @param uri     the WebSocket endpoint URI
     * @param handler the session handler
     * @return a Mono that completes when the connection is closed
     */
    Mono<Void> execute(URI uri, org.springframework.web.reactive.socket.WebSocketHandler handler);
}
