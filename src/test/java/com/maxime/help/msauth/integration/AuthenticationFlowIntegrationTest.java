package com.maxime.help.msauth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.maxime.help.msauth.application.service.TokenPair;
import com.maxime.help.msauth.domain.event.LoggedOutEvent;
import com.maxime.help.msauth.domain.event.LoginSucceededEvent;
import com.maxime.help.msauth.domain.event.TokenRefreshedEvent;
import com.maxime.help.msauth.domain.event.UserRegisteredEvent;
import com.maxime.help.msauth.domain.model.RefreshToken;
import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.domain.port.out.RefreshTokenRepository;
import com.maxime.help.msauth.domain.port.out.TokenHasher;
import com.maxime.help.msauth.domain.port.out.UserRepository;
import com.maxime.help.msauth.web.dto.LogoutRequest;
import com.maxime.help.msauth.web.dto.RefreshRequest;
import com.maxime.help.msauth.web.dto.SigninRequest;
import com.maxime.help.msauth.web.dto.SignupRequest;
import com.maxime.help.msauth.web.dto.TokenPairResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Drives the real HTTP layer against a real MySQL + real Kafka broker (no mocking of infra) for
 * one continuous signup -> login -> refresh -> logout flow, asserting the MySQL rows and Kafka
 * events each step is supposed to produce. See AuthenticationServiceTest/AuthControllerTest for
 * the mocked unit-level coverage of the same behavior; this class only proves the wiring.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationFlowIntegrationTest {

    // apache/kafka:3.9.0 (compose.yaml's dev version) doesn't work with Testcontainers'
    // KafkaContainer: its entrypoint doesn't honor the CMD override Testcontainers relies on to
    // inject KAFKA_ADVERTISED_LISTENERS, so the broker falls back to advertising 0.0.0.0 and
    // refuses to start. 4.0.0's entrypoint does honor it.
    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Container
    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.0.0");

    private static final List<String> AUTH_TOPICS =
            List.of("auth.user.registered", "auth.login.succeeded", "auth.token.refreshed", "auth.logout");

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    TokenHasher tokenHasher;

    @Autowired
    PlatformTransactionManager transactionManager;

    // Not @Autowired: this app context has no ObjectMapper bean (webmvc's Jackson support here
    // doesn't expose one), so the test builds its own, registering the same jsr310 module the
    // app relies on to serialize the events' Instant fields onto Kafka.
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private RestTestClient client;
    private KafkaConsumer<String, String> eventConsumer;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        transactionTemplate = new TransactionTemplate(transactionManager);

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "auth-flow-it-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        eventConsumer = new KafkaConsumer<>(consumerProps);
        eventConsumer.subscribe(AUTH_TOPICS);
    }

    @AfterEach
    void tearDown() {
        eventConsumer.close();
    }

    @Test
    void signupThenLoginThenRefreshThenLogout_persistsMySqlStateAndPublishesKafkaEventsAtEachStep() {
        String email = "flow-" + UUID.randomUUID() + "@example.com";
        String password = "Passw0rd1!!";

        // --- Step 1: signup creates the user and publishes UserRegisteredEvent ---
        UUID userId = whenUserSignsUp(email, password, "Alice", "Smith");
        thenUserRowIsPersisted(userId, email, "Alice", "Smith");
        thenEventIsPublished(
                "auth.user.registered",
                userId.toString(),
                UserRegisteredEvent.class,
                event -> assertThat(event.email()).isEqualTo(email));

        // --- Step 2: login issues a token pair and publishes LoginSucceededEvent ---
        TokenPair firstLogin = whenUserLogsIn(email, password);
        thenRefreshTokenRowIsPersisted(userId, firstLogin.refreshToken(), /* expectedRevoked */ false);
        thenEventIsPublished(
                "auth.login.succeeded",
                userId.toString(),
                LoginSucceededEvent.class,
                event -> assertThat(event.userId()).isEqualTo(userId));

        // --- Step 3: refresh rotates the token and publishes TokenRefreshedEvent ---
        TokenPair refreshed = whenUserRefreshesToken(firstLogin.refreshToken());
        thenRefreshTokenRowIsRevoked(firstLogin.refreshToken());
        thenRefreshTokenRowIsPersisted(userId, refreshed.refreshToken(), /* expectedRevoked */ false);
        thenEventIsPublished(
                "auth.token.refreshed",
                userId.toString(),
                TokenRefreshedEvent.class,
                event -> assertThat(event.userId()).isEqualTo(userId));

        // --- Step 4: logout revokes the current token and publishes LoggedOutEvent ---
        whenUserLogsOut(refreshed.accessToken(), refreshed.refreshToken());
        thenRefreshTokenRowIsRevoked(refreshed.refreshToken());
        thenEventIsPublished(
                "auth.logout",
                userId.toString(),
                LoggedOutEvent.class,
                event -> assertThat(event.userId()).isEqualTo(userId));
    }

    // --- when: perform the HTTP call, assert its status, return what the next step needs ---

    private UUID whenUserSignsUp(String email, String password, String firstName, String lastName) {
        client.post()
                .uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SignupRequest(email, password, firstName, lastName))
                .exchange()
                .expectStatus()
                .isCreated();

        // A fresh transaction per read (rather than one spanning the whole test method) so each
        // lookup sees the latest committed state instead of a stale REPEATABLE READ snapshot
        // from before later steps' HTTP-triggered commits. Needed here specifically because
        // UserRepository's mapping touches the lazy Profile association, which requires an open
        // Hibernate session — see UserRepositoryAdapter.findByEmail / CLAUDE.md's note on the
        // same LazyInitializationException in AuthenticationService.login().
        return transactionTemplate.execute(
                status -> userRepository.findByEmail(email).orElseThrow().getId());
    }

    private TokenPair whenUserLogsIn(String email, String password) {
        TokenPairResponse response = client.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SigninRequest(email, password))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TokenPairResponse.class)
                .returnResult()
                .getResponseBody();

        return new TokenPair(response.accessToken(), response.refreshToken());
    }

    private TokenPair whenUserRefreshesToken(String rawRefreshToken) {
        TokenPairResponse response = client.post()
                .uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshRequest(rawRefreshToken))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TokenPairResponse.class)
                .returnResult()
                .getResponseBody();

        return new TokenPair(response.accessToken(), response.refreshToken());
    }

    private void whenUserLogsOut(String accessToken, String rawRefreshToken) {
        client.post()
                .uri("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LogoutRequest(rawRefreshToken))
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    // --- then: assert real MySQL rows / real Kafka messages via the actual domain ports ---

    private void thenUserRowIsPersisted(UUID userId, String email, String firstName, String lastName) {
        // Same fresh-transaction reasoning as whenUserSignsUp: needed for the lazy Profile load.
        transactionTemplate.executeWithoutResult(status -> {
            User user = userRepository.findById(userId).orElseThrow();
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getProfile().getFirstName()).isEqualTo(firstName);
            assertThat(user.getProfile().getLastName()).isEqualTo(lastName);
            assertThat(user.hasPassword()).isTrue();
        });
    }

    private void thenRefreshTokenRowIsPersisted(UUID userId, String rawToken, boolean expectedRevoked) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken)).orElseThrow();
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.isRevoked()).isEqualTo(expectedRevoked);
        assertThat(token.getExpiresAt()).isAfter(Instant.now());
    }

    private void thenRefreshTokenRowIsRevoked(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken)).orElseThrow();
        assertThat(token.isRevoked()).isTrue();
    }

    private <T> void thenEventIsPublished(
            String topic, String expectedKey, Class<T> eventType, Consumer<T> assertions) {
        AtomicReference<T> found = new AtomicReference<>();
        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = eventConsumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals(topic) && expectedKey.equals(record.key())) {
                    found.set(objectMapper.readValue(record.value(), eventType));
                }
            }
            assertThat(found.get()).isNotNull();
        });
        assertions.accept(found.get());
    }
}
