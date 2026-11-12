package com.universalfit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universalfit.AbstractMongoIntegrationTest;
import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import com.universalfit.integration.engine.EngineClient;
import com.universalfit.integration.engine.EngineDtos;
import com.universalfit.integration.engine.EngineRejectedRequestException;
import com.universalfit.repository.*;
import com.universalfit.seed.DemoDataSeeder;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Real HTTP layer + real MongoDB (Testcontainers) + the real DemoDataSeeder. Only the Python engine is faked.
 * Covers: register -> consent -> save measurements -> pick the demo T-shirt -> Find My Size -> persisted result.
 */
class DemoTshirtRecommendationFlowTest extends AbstractMongoIntegrationTest {

    @MockBean EngineClient engineClient;

    @Autowired BrandRepository brands;
    @Autowired SizeChartRepository charts;
    @Autowired ProductRepository products;
    @Autowired ProductSizeSpecRepository specs;
    @Autowired FitRecommendationRepository recommendations;

    private ObjectId demoTeeId;

    @BeforeEach
    void seedDemoData() {
        new DemoDataSeeder(brands, charts, products, specs).seed();
        demoTeeId = products.findAll().stream()
                .filter(p -> p.getName().startsWith("DEMO Alpha Wear Everyday Tee"))
                .findFirst().orElseThrow().getId();
    }

    private String userWithConsentAndMeasurements(String email) throws Exception {
        String token = register(email).get("accessToken").asText();
        mockMvc.perform(put("/api/measurements/consent")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"consent\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/measurements/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unit\":\"CM\",\"preferredFit\":\"REGULAR\",\"weightKg\":70,"
                                + "\"values\":{\"height\":175,\"chest\":96,\"waist\":82,\"shoulder\":44}}"))
                .andExpect(status().isOk());
        return token;
    }

    private static EngineDtos.RecommendationResponse engineAnswer() {
        return new EngineDtos.RecommendationResponse("M", 0.82, false, FitPreference.REGULAR, FitPreference.REGULAR,
                "Chest and shoulder fit well.", List.of("L"), Map.of("chest", "good"),
                Map.of("S", 0.4, "M", 0.9, "L", 0.7, "XL", 0.2),
                List.of("chest", "waist", "shoulder"), List.of(), List.of(), Map.of());
    }

    @Test
    @DisplayName("Demo T-shirt: valid engine request is built from seeded data and the result is persisted")
    void demoTshirtEndToEnd() throws Exception {
        String token = userWithConsentAndMeasurements("flow@example.com");
        when(engineClient.recommend(any())).thenReturn(engineAnswer());

        mockMvc.perform(post("/api/recommendations/" + demoTeeId.toHexString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedSize").value("M"))
                .andExpect(jsonPath("$.confidence").value(0.82));

        ArgumentCaptor<EngineDtos.RecommendRequest> sent = ArgumentCaptor.forClass(EngineDtos.RecommendRequest.class);
        verify(engineClient).recommend(sent.capture());
        EngineDtos.RecommendRequest request = sent.getValue();
        assertThat(request.category()).isEqualTo(Category.T_SHIRT);
        assertThat(request.productFitType()).isEqualTo(FitPreference.REGULAR);
        assertThat(request.measurementBasis()).isEqualTo(MeasurementBasis.GARMENT);
        assertThat(request.unit()).isEqualTo(Unit.CM);
        assertThat(request.chartVerified()).isFalse();
        assertThat(request.sizes()).extracting(EngineDtos.Size::label).containsExactly("S", "M", "L", "XL");
        request.sizes().forEach(size ->
                assertThat(size.measurements().keySet()).containsExactlyInAnyOrder("chest", "waist", "shoulder"));
        assertThat(request.profile().chest()).isEqualTo(96.0);
        assertThat(request.profile().weight()).isEqualTo(70.0);

        User user = userRepository.findByEmail("flow@example.com").orElseThrow();
        List<FitRecommendation> saved = recommendations.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getUserId()).isEqualTo(user.getId());
        assertThat(saved.get(0).getProductId()).isEqualTo(demoTeeId);
        assertThat(saved.get(0).getRecommendedSize()).isEqualTo("M");
        assertThat(saved.get(0).getSizeChartId()).as("product-specific spec used").isNull();
    }

    @Test
    @DisplayName("Engine 422 surfaces as 422 (not 502), generic message, nothing persisted")
    void engineRejectionIsNotABadGatewayAndSavesNothing() throws Exception {
        String token = userWithConsentAndMeasurements("rejected@example.com");
        when(engineClient.recommend(any())).thenThrow(new EngineRejectedRequestException("rejected"));

        mockMvc.perform(post("/api/recommendations/" + demoTeeId.toHexString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableEntity());

        assertThat(recommendations.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Without measurement consent the engine is never called")
    void consentIsStillEnforced() throws Exception {
        String token = register("noconsent@example.com").get("accessToken").asText();

        mockMvc.perform(post("/api/recommendations/" + demoTeeId.toHexString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(engineClient);
        assertThat(recommendations.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Measurements the engine would reject are refused when saved, not later at recommendation time")
    void implausibleMeasurementIsRefusedAtSaveTime() throws Exception {
        String token = register("implausible@example.com").get("accessToken").asText();
        mockMvc.perform(put("/api/measurements/consent")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"consent\":true}"))
                .andExpect(status().isOk());

        // 40 cm chest: the old Java range (30-200) accepted it, the engine (50-200) rejects it.
        mockMvc.perform(put("/api/measurements/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unit\":\"CM\",\"values\":{\"chest\":40}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for chest in CM"));

        // Weight outside the engine's 20-300 kg.
        mockMvc.perform(put("/api/measurements/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unit\":\"CM\",\"weightKg\":10,\"values\":{\"chest\":96}}"))
                .andExpect(status().isBadRequest());
    }
}
