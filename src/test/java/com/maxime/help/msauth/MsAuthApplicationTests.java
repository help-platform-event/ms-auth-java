package com.maxime.help.msauth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest
class MsAuthApplicationTests {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    // apache/kafka:3.9.0 (compose.yaml's dev version) doesn't work with Testcontainers'
    // KafkaContainer: its entrypoint doesn't honor the CMD override Testcontainers relies on to
    // inject KAFKA_ADVERTISED_LISTENERS, so the broker falls back to advertising 0.0.0.0 and
    // refuses to start. 4.0.0's entrypoint does honor it — verified directly against this Docker
    // daemon before wiring this in.
    @Container
    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.0.0");

    @Test
    void contextLoads() {
    }
}
