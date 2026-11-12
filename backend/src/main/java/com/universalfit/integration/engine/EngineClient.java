
package com.universalfit.integration.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class EngineClient {

    private static final Logger log = LoggerFactory.getLogger(EngineClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public EngineClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${universalfit.engine.base-url}") String baseUrl,
            @Value("${universalfit.engine.api-key:}") String apiKey) {

        this.objectMapper = objectMapper;
        this.apiKey = apiKey;

        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    public EngineDtos.RecommendationResponse recommend(
            EngineDtos.RecommendRequest request) {

        final byte[] requestBody;

        try {
            requestBody = objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException ex) {

            log.error(
                    "Failed to serialize recommendation request: {}",
                    ex.getClass().getSimpleName());

            throw new EngineResponseException(
                    "Failed to prepare recommendation request.");
        }

        try {
            return restClient.post()
                    .uri("/v1/recommend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.set("X-API-Key", apiKey);
                        }
                    })
                    .body(requestBody)
                    .retrieve()
                    .body(EngineDtos.RecommendationResponse.class);

        } catch (ResourceAccessException ex) {

            log.error(
                    "Recommendation engine connection failed: {}",
                    ex.getClass().getSimpleName());

            throw new EngineUnavailableException(
                    "Recommendation engine is unavailable.");

        } catch (RestClientResponseException ex) {

            log.error(
                    "Recommendation engine returned HTTP status {}",
                    ex.getStatusCode().value());

            if (ex.getStatusCode().is5xxServerError()) {
                throw new EngineUnavailableException(
                        "Recommendation engine is unavailable.");
            }

            if (ex.getStatusCode().value() == 400
                    || ex.getStatusCode().value() == 422) {
                throw new EngineRejectedRequestException(
                        "Recommendation engine rejected the request.");
            }

            throw new EngineResponseException(
                    "Recommendation engine rejected the request.");

        } catch (RestClientException ex) {

            log.error(
                    "Recommendation engine client error: {}",
                    ex.getClass().getSimpleName());

            throw new EngineResponseException(
                    "Recommendation engine returned an invalid response.");
        }
    }
}
