package com.universalfit.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.universalfit.AbstractMongoIntegrationTest;
import com.universalfit.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Integration tests for the JWT security rules. Runs only against its own Testcontainers MongoDB
 * (see {@link AbstractMongoIntegrationTest}); it can never touch the Docker Compose development database.
 */
class AuthSecurityTest extends AbstractMongoIntegrationTest {

    private ResultActions getMe(String token) throws Exception {
        return mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"));
    }

    // ----- 1-3: token types -----------------------------------------------------------------

    @Test
    void accessTokenIsAcceptedByProtectedEndpoints() throws Exception {
        JsonNode tokens = register("access-ok@example.com");
        getMe(tokens.get("accessToken").asText())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("access-ok@example.com"));
    }

    @Test
    void refreshTokenIsRejectedByProtectedEndpoints() throws Exception {
        JsonNode tokens = register("refresh-as-access@example.com");
        getMe(tokens.get("refreshToken").asText()).andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenIsRejectedByRefreshEndpoint() throws Exception {
        JsonNode tokens = register("access-as-refresh@example.com");
        refresh(tokens.get("accessToken").asText())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void refreshTokenIsAcceptedByRefreshEndpointAndIssuesWorkingTokens() throws Exception {
        JsonNode tokens = register("refresh-ok@example.com");
        String body = refresh(tokens.get("refreshToken").asText())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode renewed = JSON.readTree(body);
        getMe(renewed.get("accessToken").asText()).andExpect(status().isOk());
        // The newly issued refresh token must not work as an access token either.
        getMe(renewed.get("refreshToken").asText()).andExpect(status().isUnauthorized());
    }

    // ----- bad tokens ----------------------------------------------------------------------

    @Test
    void garbageTokensAreRejectedEverywhereWithoutServerErrors() throws Exception {
        getMe("not-a-jwt").andExpect(status().isUnauthorized());
        getMe("a.b.c").andExpect(status().isUnauthorized());
        refresh("not-a-jwt").andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    // ----- 4-5: deleted users -----------------------------------------------------------------

    @Test
    void tokensOfADeletedUserGet401NotA500() throws Exception {
        JsonNode tokens = register("deleted@example.com");
        userRepository.deleteAll();

        getMe(tokens.get("accessToken").asText()).andExpect(status().isUnauthorized());
        refresh(tokens.get("refreshToken").asText())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    // ----- 4-5: disabled users ----------------------------------------------------------------

    @Test
    void disablingAUserStopsTheirExistingTokensImmediately() throws Exception {
        JsonNode tokens = register("disabled@example.com");
        getMe(tokens.get("accessToken").asText()).andExpect(status().isOk()); // works before disabling

        User user = userRepository.findByEmail("disabled@example.com").orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        getMe(tokens.get("accessToken").asText()).andExpect(status().isUnauthorized());
        refresh(tokens.get("refreshToken").asText()).andExpect(status().isUnauthorized());
        login("disabled@example.com").andExpect(status().isUnauthorized());
    }

    @Test
    void reEnablingAUserRestoresAccess() throws Exception {
        JsonNode tokens = register("reenabled@example.com");
        User user = userRepository.findByEmail("reenabled@example.com").orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);
        getMe(tokens.get("accessToken").asText()).andExpect(status().isUnauthorized());

        user.setEnabled(true);
        userRepository.save(user);
        getMe(tokens.get("accessToken").asText()).andExpect(status().isOk());
    }
}
