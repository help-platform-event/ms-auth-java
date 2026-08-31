package com.maxime.help.msauth.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maxime.help.msauth.domain.port.out.GoogleAuthenticationException;
import com.maxime.help.msauth.domain.port.out.GoogleIdentity;
import com.maxime.help.msauth.domain.port.out.GoogleIdentityProvider;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Exchanges a Google JS-SDK ("postmessage" flow) authorization code for a verified identity.
 * Reuses the auto-configured {@link ClientRegistrationRepository} purely as a config source
 * (client id/secret/token-uri/jwk-set-uri via Spring's "google" common-provider preset) and does
 * its own token-endpoint call, since the original flow hands us a code obtained client-side, not
 * one Spring Security's redirect-based {@code oauth2Login()} machinery is driving.
 */
@Component
class GoogleOidcIdentityProviderAdapter implements GoogleIdentityProvider {

    private static final String REGISTRATION_ID = "google";

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RestClient restClient;
    private final String redirectUri;
    private final JwtDecoder fixedDecoder;

    @Autowired
    GoogleOidcIdentityProviderAdapter(
            ClientRegistrationRepository clientRegistrationRepository, AuthProperties properties) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.restClient = RestClient.builder().build();
        this.redirectUri = properties.google().redirectUri();
        this.fixedDecoder = null;
    }

    /** Test-only: bypasses registration lookup and JWKS wiring with a pre-built decoder. */
    GoogleOidcIdentityProviderAdapter(
            ClientRegistrationRepository clientRegistrationRepository,
            RestClient restClient,
            String redirectUri,
            JwtDecoder fixedDecoder) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.restClient = restClient;
        this.redirectUri = redirectUri;
        this.fixedDecoder = fixedDecoder;
    }

    @Override
    public GoogleIdentity exchangeAuthorizationCode(String authorizationCode) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID);
        if (registration == null) {
            throw new GoogleAuthenticationException("Google OAuth client is not configured");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", authorizationCode);
        form.add("client_id", registration.getClientId());
        form.add("client_secret", registration.getClientSecret());
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        TokenResponse response;
        try {
            response =
                    restClient
                            .post()
                            .uri(registration.getProviderDetails().getTokenUri())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(form)
                            .retrieve()
                            .body(TokenResponse.class);
        } catch (RestClientException e) {
            throw new GoogleAuthenticationException("Failed to exchange authorization code with Google", e);
        }
        if (response == null || response.idToken() == null) {
            throw new GoogleAuthenticationException("Google did not return an id_token");
        }

        JwtDecoder decoder =
                fixedDecoder != null
                        ? fixedDecoder
                        : NimbusJwtDecoder.withJwkSetUri(registration.getProviderDetails().getJwkSetUri()).build();

        Jwt jwt;
        try {
            jwt = decoder.decode(response.idToken());
        } catch (JwtException e) {
            throw new GoogleAuthenticationException("Invalid Google id_token", e);
        }

        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
        if (issuer == null
                || !("https://accounts.google.com".equals(issuer) || "accounts.google.com".equals(issuer))) {
            throw new GoogleAuthenticationException("Unexpected Google id_token issuer: " + issuer);
        }
        List<String> audience = jwt.getAudience();
        if (audience == null || !audience.contains(registration.getClientId())) {
            throw new GoogleAuthenticationException("Google id_token audience mismatch");
        }

        return new GoogleIdentity(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")));
    }

    private record TokenResponse(@JsonProperty("id_token") String idToken) {}
}
