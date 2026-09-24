package com.maxime.help.msauth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxime.help.msauth.domain.model.Availability;
import com.maxime.help.msauth.domain.model.NotificationSettings;
import com.maxime.help.msauth.domain.port.out.UserRepository;
import com.maxime.help.msauth.domain.port.out.UserSettingsRepository;
import com.maxime.help.msauth.web.dto.SigninRequest;
import com.maxime.help.msauth.web.dto.SignupRequest;
import com.maxime.help.msauth.web.dto.TokenPairResponse;
import java.util.UUID;
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
 * Drives the profile, settings and user-lookup endpoints over the real HTTP layer against a real
 * MySQL (and Kafka, which signup/login publish to). Proves the V2 migration, the Hibernate mapping
 * of {@code user_settings}, and the batch lookup's fetch graph; see MeControllerTest /
 * UserControllerTest for the mocked coverage of the JSON contract.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserSettingsFlowIntegrationTest {

    // Same images as AuthenticationFlowIntegrationTest (see there for why Kafka is pinned to 4.0.0).
    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Container
    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.0.0");

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserSettingsRepository userSettingsRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    private RestTestClient client;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void profileSettingsAndLookups_roundTripThroughMySql() {
        String email = "settings-" + UUID.randomUUID() + "@example.com";
        UUID userId = signUp(email, "Alice", "Smith");
        String bearer = "Bearer " + logIn(email);

        // --- Profile: full replacement, address with coordinates, read back ---
        client.patch()
                .uri("/api/me/profile")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"firstName":"Alicia","lastName":"Smith","bio":"Hello",
                         "address":{"streetNumber":"12","streetName":"Rue de Paris","city":"Lille",
                                    "postalCode":"59000","country":"France",
                                    "coordinates":{"lat":50.629250,"lon":3.057256}}}
                        """)
                .exchange()
                .expectStatus()
                .isOk();

        client.get()
                .uri("/api/me/profile")
                .header("Authorization", bearer)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.firstName").isEqualTo("Alicia")
                .jsonPath("$.bio").isEqualTo("Hello")
                .jsonPath("$.address.city").isEqualTo("Lille")
                .jsonPath("$.address.coordinates.lat").isEqualTo(50.62925)
                .jsonPath("$.avatarUrl").doesNotExist();

        // --- Availability: defaults without a row, then no row is written by a read ---
        client.get()
                .uri("/api/me/availability")
                .header("Authorization", bearer)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.monday").isEqualTo(true)
                .jsonPath("$.sunday").isEqualTo(true);
        assertThat(userSettingsRepository.findByUserId(userId)).isEmpty();

        // --- Notifications: first update creates the row, availability keeps its defaults ---
        client.patch()
                .uri("/api/me/notifications")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"enabled":false,"eventActivity":true,"eventMessages":false,"documents":true,
                         "deadlines":true,"nearbyEvents":false,"judgments":true}
                        """)
                .exchange()
                .expectStatus()
                .isOk();

        // --- Availability: second update goes through the update path of the same row ---
        client.patch()
                .uri("/api/me/availability")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"monday":true,"tuesday":true,"wednesday":true,"thursday":true,"friday":true,
                         "saturday":false,"sunday":false}
                        """)
                .exchange()
                .expectStatus()
                .isOk();

        var settings = userSettingsRepository.findByUserId(userId).orElseThrow();
        assertThat(settings.getNotifications())
                .isEqualTo(new NotificationSettings(false, true, false, true, true, false, true));
        assertThat(settings.getAvailability())
                .isEqualTo(new Availability(true, true, true, true, true, false, false));

        // --- Lookups used by the Gateway ---
        client.get()
                .uri("/api/users/{id}", userId)
                .header("Authorization", bearer)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.first_name").isEqualTo("Alicia");

        client.get()
                .uri("/api/users/profiles?ids={a},{b}", userId, UUID.randomUUID())
                .header("Authorization", bearer)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo(userId.toString())
                .jsonPath("$[0].email").isEqualTo(email);

        // --- Error statuses must survive the servlet container's error dispatch to /error ---
        client.patch()
                .uri("/api/me/availability")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"monday\":true}")
                .exchange()
                .expectStatus()
                .isBadRequest();

        client.get()
                .uri("/api/users")
                .header("Authorization", bearer)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    private UUID signUp(String email, String firstName, String lastName) {
        client.post()
                .uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SignupRequest(email, "Passw0rd1!!", firstName, lastName))
                .exchange()
                .expectStatus()
                .isCreated();
        // Fresh transaction: mapping the user touches the lazy Profile association.
        return transactionTemplate.execute(status -> userRepository.findByEmail(email).orElseThrow().getId());
    }

    private String logIn(String email) {
        return client.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SigninRequest(email, "Passw0rd1!!"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TokenPairResponse.class)
                .returnResult()
                .getResponseBody()
                .accessToken();
    }
}
