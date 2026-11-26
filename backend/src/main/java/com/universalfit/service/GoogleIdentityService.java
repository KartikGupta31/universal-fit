package com.universalfit.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Verifies Google Identity Services ID tokens with Google's tokeninfo endpoint.
 * The token's audience must match this application's configured OAuth client ID.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleIdentityService {
    private final RestClient.Builder restClientBuilder;

    @Value("${universalfit.google.client-id:}")
    private String clientId;

    public Optional<GoogleIdentity> verify(String idToken) {
        if (clientId == null || clientId.isBlank() || idToken == null || idToken.isBlank()) {
            return Optional.empty();
        }
        try {
            String uri = UriComponentsBuilder
                    .fromUriString("https://oauth2.googleapis.com/tokeninfo")
                    .queryParam("id_token", idToken)
                    .build()
                    .encode()
                    .toUriString();

            JsonNode claims = restClientBuilder.build()
                    .get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);

            if (claims == null
                    || !clientId.equals(claims.path("aud").asText())
                    || !("accounts.google.com".equals(claims.path("iss").asText())
                         || "https://accounts.google.com".equals(claims.path("iss").asText()))
                    || !claims.path("email_verified").asText().equalsIgnoreCase("true")
                    || claims.path("exp").asLong(0) <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            String email = claims.path("email").asText("");
            String subject = claims.path("sub").asText("");
            String name = claims.path("name").asText("");
            if (email.isBlank() || subject.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new GoogleIdentity(email, name, subject));
        } catch (Exception exception) {
            log.warn("Google identity verification failed: {}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public record GoogleIdentity(String email, String name, String subject) {}
}
