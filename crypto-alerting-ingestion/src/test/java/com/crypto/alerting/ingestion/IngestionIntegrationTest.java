package com.crypto.alerting.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = { "topic-raw-prices" }, brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
})
@DirtiesContext
class IngestionIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void allRequiredBeansArePresent() {
        assertThat(applicationContext.containsBean("binanceWebSocketClient")).isTrue();
        assertThat(applicationContext.containsBean("priceProducer")).isTrue();
        assertThat(applicationContext.containsBean("priceStreamService")).isTrue();
        assertThat(applicationContext.containsBean("priceStreamController")).isTrue();
    }
}
