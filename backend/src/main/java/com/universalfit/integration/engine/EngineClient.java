package com.universalfit.integration.engine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import java.time.Duration;
@Component
public class EngineClient {
    private final RestClient restClient;
    private final String apiKey;
    public EngineClient(
            RestClient.Builder builder,
            @Value("${universalfit.engine.base-url}") String baseUrl,
            @Value("${universalfit.engine.api-key:}") String apiKey
    ) {
        this.apiKey = apiKey;
        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
    }
    public EngineDtos.RecommendationResponse recommend(
            EngineDtos.RecommendRequest request
    ) {
        try {
            return restClient.post()
                    .uri("/v1/recommend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.set("X-API-Key", apiKey);
                        }
                    })
                    .body(request)
                    .retrieve()
                    .body(EngineDtos.RecommendationResponse.class);
        } catch (ResourceAccessException ex) {
            throw new EngineUnavailableException(
                    "Recommendation engine is unavailable."
            );
        }
    }
}
