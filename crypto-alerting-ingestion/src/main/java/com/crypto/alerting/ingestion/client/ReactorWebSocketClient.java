package com.crypto.alerting.ingestion.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Implementation of WebSocketClient using Reactor Netty.
 * Delegates to ReactorNettyWebSocketClient for actual WebSocket operations.
 */
@Component
public final class ReactorWebSocketClient implements WebSocketClient {

    private final ReactorNettyWebSocketClient delegate;

    /**
     * Creates a new ReactorWebSocketClient with default configuration.
     */
    public ReactorWebSocketClient() {
        this.delegate = new ReactorNettyWebSocketClient();
    }

    @Override
    public Mono<Void> execute(final URI uri, final WebSocketHandler handler) {
        return delegate.execute(uri, handler);
    }
}
