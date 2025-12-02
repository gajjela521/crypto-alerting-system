# Real-Time Crypto Alerting System

A highly concurrent, low-latency system monitoring real-time cryptocurrency prices, filtering against user-defined thresholds, and pushing instantaneous alerts. Built with Spring WebFlux, Project Reactor, Kafka, and Redis.

## Architecture

- **crypto-alerting-ingestion**: Connects to Binance WebSocket, normalizes data, and publishes to Kafka (`topic-raw-prices`).
- **crypto-alerting-processor**: Consumes prices, checks against rules in Redis, and publishes alerts (`topic-alerts`).
- **crypto-alerting-rules**: Manages user rules and persists them to PostgreSQL/Redis.
- **crypto-alerting-distribution**: Pushes alerts to connected users via WebSockets.
- **crypto-alerting-commons**: Shared data models and utilities.

## Prerequisites

- Java 21+
- Docker & Docker Compose

## Getting Started

1.  **Start Infrastructure**
    ```bash
    ./start-infra.sh
    # OR if you have docker compose v2
    docker compose up -d
    ```

2.  **Build the Project**
    ```bash
    ./gradlew build
    ```

3.  **Run Ingestion Service**
    ```bash
    ./gradlew :crypto-alerting-ingestion:bootRun
    ```


## Deployment

### Docker

```bash
docker compose up --build
```

The service will be available at `http://localhost:8080/api/prices/stream`.

### GitHub Actions

Pushes to `main` trigger the CI/CD pipeline which builds the Docker image, pushes it to Docker Hub, and deploys the static UI to GitHub Pages.

## Project Structure

The project follows a multi-module Gradle structure:
- `crypto-alerting-commons`: Shared code.
- `crypto-alerting-*`: Microservices.

## Technologies
- Spring Boot 3, Spring WebFlux
- Project Reactor (Flux/Mono)
- Apache Kafka (Reactor Kafka)
- Redis (Reactive)
- PostgreSQL (R2DBC)
- **Observability**: Prometheus, Grafana, Spring Boot Actuator
- **Documentation**: SpringDoc OpenAPI (Swagger)

## Access Points
- **Swagger UI**: [http://localhost:8081/webjars/swagger-ui/index.html](http://localhost:8081/webjars/swagger-ui/index.html)
- **Prometheus**: [http://localhost:9090](http://localhost:9090)
- **Grafana**: [http://localhost:3000](http://localhost:3000) (Login: admin/admin)
- **Actuator Metrics**: [http://localhost:8081/actuator/prometheus](http://localhost:8081/actuator/prometheus)
