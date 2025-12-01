plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":crypto-alerting-commons"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.projectreactor.kafka:reactor-kafka")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
