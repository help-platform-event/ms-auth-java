package com.maxime.help.msauth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.maxime.help.msauth.domain.port.out.GoogleAuthenticationException;
import com.maxime.help.msauth.domain.port.out.GoogleIdentity;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** No real HTTP/JWKS calls: the token exchange is mocked via {@link MockRestServiceServer} and
 * id_token verification via the adapter's test-only fixed-{@link JwtDecoder} constructor. */
class GoogleOidcIdentityProviderAdapterTest {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String CLIENT_ID = "test-client-id";

    private static ClientRegistration googleRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId(CLIENT_ID)
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("postmessage")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri(TOKEN_URI)
                .clientName("Google")
                .build();
    }

    private static Jwt.Builder validJwtBuilder() {
        return Jwt.withTokenValue("fake-id-token")
                .header("alg", "RS256")
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2026-01-01T01:00:00Z"))
                .claim("sub", "google-sub")
                .claim("email", "alice@example.com")
                .claim("email_verified", true);
    }

    private record Fixture(GoogleOidcIdentityProviderAdapter adapter, MockRestServiceServer server) {}

    private static Fixture fixtureWithDecoder(JwtDecoder decoder) {
        ClientRegistrationRepository repository = mock(ClientRegistrationRepository.class);
        when(repository.findByRegistrationId("google")).thenReturn(googleRegistration());

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        return new Fixture(
                new GoogleOidcIdentityProviderAdapter(repository, restClient, "postmessage", decoder),
                server);
    }

    @Test
    void exchangeAuthorizationCode_returnsIdentityForValidCode() {
        Jwt jwt =
                validJwtBuilder()
                        .claim("iss", "https://accounts.google.com")
                        .claim("aud", List.of(CLIENT_ID))
                        .build();
        Fixture fixture = fixtureWithDecoder(token -> jwt);
        fixture
                .server()
                .expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess("{\"id_token\":\"fake-id-token\"}", MediaType.APPLICATION_JSON));

        GoogleIdentity identity = fixture.adapter().exchangeAuthorizationCode("auth-code");

        assertThat(identity).isEqualTo(new GoogleIdentity("google-sub", "alice@example.com", true));
    }

    @Test
    void exchangeAuthorizationCode_throwsWhenIssuerIsUnexpected() {
        Jwt jwt =
                validJwtBuilder().claim("iss", "https://evil.example.com").claim("aud", List.of(CLIENT_ID)).build();
        Fixture fixture = fixtureWithDecoder(token -> jwt);
        fixture
                .server()
                .expect(requestTo(TOKEN_URI))
                .andRespond(
                        withSuccess("{\"id_token\":\"fake-id-token\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.adapter().exchangeAuthorizationCode("auth-code"))
                .isInstanceOf(GoogleAuthenticationException.class);
    }

    @Test
    void exchangeAuthorizationCode_throwsWhenAudienceMismatches() {
        Jwt jwt =
                validJwtBuilder()
                        .claim("iss", "https://accounts.google.com")
                        .claim("aud", List.of("someone-elses-client-id"))
                        .build();
        Fixture fixture = fixtureWithDecoder(token -> jwt);
        fixture
                .server()
                .expect(requestTo(TOKEN_URI))
                .andRespond(
                        withSuccess("{\"id_token\":\"fake-id-token\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.adapter().exchangeAuthorizationCode("auth-code"))
                .isInstanceOf(GoogleAuthenticationException.class);
    }

    @Test
    void exchangeAuthorizationCode_throwsWhenGoogleReturnsNoIdToken() {
        Fixture fixture =
                fixtureWithDecoder(
                        token -> {
                            throw new AssertionError("decoder should not be called without an id_token");
                        });
        fixture.server().expect(requestTo(TOKEN_URI)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.adapter().exchangeAuthorizationCode("auth-code"))
                .isInstanceOf(GoogleAuthenticationException.class);
    }
}
