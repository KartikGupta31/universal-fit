package com.universalfit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universalfit.AbstractMongoIntegrationTest;
import com.universalfit.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Registration, login and refresh behaviour. Extends {@link AbstractMongoIntegrationTest}, so it only ever uses
 * its own Testcontainers MongoDB and cleans only that database (never the Docker Compose development one).
 */
class AuthControllerTest extends AbstractMongoIntegrationTest {

    @Test
    void registrationSuccess() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"Secret123!\",\"displayName\":\"Test User\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void duplicateEmailRejected() throws Exception {
        userRepository.save(User.builder().email("dup@example.com").passwordHash("hash").build());
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@example.com\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    @Test
    void loginSuccess() throws Exception {
        register("login@example.com");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void loginInvalidCredentials() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nonexistent@example.com\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void protectedEndpointRequiresAuth() throws Exception {
        // A real protected endpoint (the old test used a path that does not exist, so it proved nothing).
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/measurements/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenRotation() throws Exception {
        String refresh = register("refresh@example.com").get("refreshToken").asText();
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void passwordHashIsNeverReturned() throws Exception {
        String access = register("nohash@example.com").get("accessToken").asText();
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(access)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
