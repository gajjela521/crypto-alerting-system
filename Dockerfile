FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
# Build the project without running tests (tests already run in CI)
RUN ./gradlew clean build -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/crypto-alerting-ingestion/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
