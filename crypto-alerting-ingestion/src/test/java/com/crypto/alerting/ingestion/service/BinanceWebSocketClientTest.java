package com.crypto.alerting.ingestion.service;

import com.crypto.alerting.ingestion.client.WebSocketClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BinanceWebSocketClientTest {

    @Mock
    private PriceProducer priceProducer;

    @Mock
    private PriceStreamService priceStreamService;

    @Mock
    private WebSocketClient webSocketClient;

    private BinanceWebSocketClient binanceWebSocketClient;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

        binanceWebSocketClient = new BinanceWebSocketClient(
                priceProducer,
                priceStreamService,
                circuitBreakerRegistry,
                retryRegistry,
                rateLimiterRegistry,
                webSocketClient);
    }

    @Test
    void connect_shouldRetryOnFailure() {
        // Arrange
        when(webSocketClient.execute(any(URI.class), any()))
                .thenReturn(Mono.error(new RuntimeException("Connection failed")))
                .thenReturn(Mono.error(new RuntimeException("Connection failed")))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(binanceWebSocketClient.connectWithResilience())
                .verifyComplete();

        // Verify that execute was called 3 times (initial + 2 retries)
        verify(webSocketClient, times(3)).execute(any(URI.class), any());
    }
}
