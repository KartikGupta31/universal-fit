package com.universalfit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universalfit.AbstractMongoIntegrationTest;
import com.universalfit.domain.model.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * A missing or null "consent" must be a 400 and must NEVER be treated as "false" (which deletes the user's
 * measurements). Runs only against its own Testcontainers MongoDB.
 */
class MeasurementConsentSafetyTest extends AbstractMongoIntegrationTest {

    private static final String EMAIL = "consent@example.com";
    private String accessToken;

    @BeforeEach
    void userWithConsentAndSavedMeasurements() throws Exception {
        accessToken = register(EMAIL).get("accessToken").asText();
        consent("{\"consent\":true}").andExpect(status().isOk());
        mockMvc.perform(authed(put("/api/measurements/me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unit\":\"CM\",\"preferredFit\":\"REGULAR\","
                                + "\"values\":{\"chest\":100,\"waist\":85}}"))
                .andExpect(status().isOk());
        assertMeasurementDataIntact();
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(accessToken));
    }

    private ResultActions consent(String jsonBody) throws Exception {
        return mockMvc.perform(authed(put("/api/measurements/consent"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody));
    }

    private void assertMeasurementDataIntact() {
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.isMeasurementConsent()).as("consent must still be true").isTrue();
        assertThat(profileRepository.findByUserId(user.getId())).as("profile must still exist").isPresent();
        assertThat(historyRepository.findAll()).as("history must still exist").isNotEmpty();
    }

    @Test
    void missingConsentFieldIsA400AndDeletesNothing() throws Exception {
        for (String badBody : List.of(
                "{}",
                "{\"consent\":null}",
                "{\"concent\":false}",          // typo of the field name
                "{\"consent\":\"maybe\"}",      // not a boolean
                "null",
                "")) {                          // no body at all
            consent(badBody).andExpect(status().isBadRequest());
            assertMeasurementDataIntact();
        }
    }

    @Test
    void emptyObjectReturnsAHelpfulMessage() throws Exception {
        consent("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "The 'consent' field is required and must be true or false"));
    }

    @Test
    void explicitFalseRevokesConsentAndDeletesMeasurementData() throws Exception {
        consent("{\"consent\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurementConsent").value(false));

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.isMeasurementConsent()).isFalse();
        assertThat(profileRepository.findByUserId(user.getId())).isEmpty();
        assertThat(historyRepository.findAll()).isEmpty();
    }

    @Test
    void explicitTrueGrantsConsentAndKeepsData() throws Exception {
        consent("{\"consent\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurementConsent").value(true));
        assertMeasurementDataIntact();
    }

    @Test
    void consentEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/measurements/consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consent\":false}"))
                .andExpect(status().isUnauthorized());
        assertMeasurementDataIntact();
    }

    @Test
    void existingMeasurementSavingStillWorks() throws Exception {
        mockMvc.perform(authed(put("/api/measurements/me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unit\":\"IN\",\"values\":{\"chest\":40,\"waist\":32}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unit").value("IN"));
    }
}
