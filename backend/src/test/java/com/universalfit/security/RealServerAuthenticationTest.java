package com.universalfit.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.universalfit.AbstractMongoIntegrationTest;
import com.universalfit.UniversalFitApplication;
import com.universalfit.domain.model.User;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Sends REAL HTTP requests to a real embedded Tomcat.
 *
 * <p>MockMvc cannot be used for this: it never performs the servlet container's ERROR dispatch to
 * {@code /error}, which is exactly where a controller's 400/403/404 used to be turned into 401. Nothing
 * here mocks the security filter chain, the JWT code or the services; tokens are issued by the real
 * {@code /auth/register} endpoint. The database is the throwaway Testcontainers one, guarded by
 * {@link AbstractMongoIntegrationTest}.
 */
@SpringBootTest(classes = UniversalFitApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealServerAuthenticationTest extends AbstractMongoIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private int send(String method, String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (token != null) {
            request.header("Authorization", bearer(token));
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private String validProductId() {
        return new ObjectId().toHexString();
    }

    // ----- rejected credentials -> 401 -----------------------------------------------------

    @Test
    @DisplayName("No Authorization header: 401 on both protected endpoints")
    void missingHeader() throws Exception {
        assertThat(send("GET", "/api/users/me", null)).isEqualTo(401);
        assertThat(send("POST", "/api/recommendations/" + validProductId(), null)).isEqualTo(401);
    }

    @Test
    @DisplayName("Invalid JWT: 401 on both protected endpoints")
    void invalidJwt() throws Exception {
        assertThat(send("GET", "/api/users/me", "not-a-jwt")).isEqualTo(401);
        assertThat(send("POST", "/api/recommendations/" + validProductId(), "a.b.c")).isEqualTo(401);
    }

    @Test
    @DisplayName("Refresh token used as access token: 401 on both protected endpoints")
    void refreshTokenAsAccessToken() throws Exception {
        String refresh = register("real-refresh@example.com").get("refreshToken").asText();

        assertThat(send("GET", "/api/users/me", refresh)).isEqualTo(401);
        assertThat(send("POST", "/api/recommendations/" + validProductId(), refresh)).isEqualTo(401);
    }

    // ----- valid access token ---------------------------------------------------------------

    @Test
    @DisplayName("Valid access token on /api/users/me: 200")
    void validAccessTokenOnMe() throws Exception {
        String access = register("real-me@example.com").get("accessToken").asText();

        assertThat(send("GET", "/api/users/me", access)).isEqualTo(200);
    }

    @Test
    @DisplayName("Valid access token + invalid product id: 400, not 401 (the reported bug)")
    void validAccessTokenInvalidProductId() throws Exception {
        String access = register("real-invalid-id@example.com").get("accessToken").asText();

        assertThat(send("POST", "/api/recommendations/invalid-id", access)).isEqualTo(400);
    }

    @Test
    @DisplayName("Valid access token + valid product id, no consent: reaches the service, 403")
    void validAccessTokenWithoutConsent() throws Exception {
        String access = register("real-no-consent@example.com").get("accessToken").asText();

        // 403 comes from RecommendationService (consent check), proving the request got past
        // authentication AND that the error status survives the ERROR dispatch.
        assertThat(send("POST", "/api/recommendations/" + validProductId(), access)).isEqualTo(403);
    }

    @Test
    @DisplayName("Valid access token + consent + valid product id: goes deeper into the service (404, no profile)")
    void validAccessTokenWithConsent() throws Exception {
        JsonNode tokens = register("real-consent@example.com");
        User user = userRepository.findByEmail("real-consent@example.com").orElseThrow();
        user.setMeasurementConsent(true);
        userRepository.save(user);

        // Consent passes; the next step of the service reports that no measurement profile exists.
        assertThat(send("POST", "/api/recommendations/" + validProductId(), tokens.get("accessToken").asText()))
                .isEqualTo(404);
    }

    // ----- protections that must not be weakened -------------------------------------------

    @Test
    @DisplayName("A disabled account is still rejected with 401")
    void disabledAccountStillRejected() throws Exception {
        String access = register("real-disabled@example.com").get("accessToken").asText();
        User user = userRepository.findByEmail("real-disabled@example.com").orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        assertThat(send("GET", "/api/users/me", access)).isEqualTo(401);
        assertThat(send("POST", "/api/recommendations/" + validProductId(), access)).isEqualTo(401);
    }

    @Test
    @DisplayName("Opening the ERROR dispatch did not make /error itself public")
    void errorEndpointIsNotPublicForDirectRequests() throws Exception {
        assertThat(send("GET", "/error", null)).isEqualTo(401);
    }
}
