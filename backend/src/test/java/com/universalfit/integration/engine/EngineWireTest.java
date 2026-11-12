package com.universalfit.integration.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Real socket, real HTTP: checks what actually arrives at the server. */
class EngineWireTest {

    private static final String OK_BODY = """
            {"recommendedSize":"M","confidence":0.8,"uncertain":false,"fitType":"REGULAR",
             "productFitType":"REGULAR","reason":"ok","alternatives":["L"],
             "measurementAnalysis":{"chest":"good"},"sizeScores":{"M":0.9},
             "measurementsUsed":["chest"],"missingMeasurements":[],"warnings":[],"dataQuality":{}}
            """;

    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<Map<String, List<String>>> seenHeaders = new AtomicReference<>();
    private final AtomicReference<byte[]> seenBody = new AtomicReference<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/recommend", exchange -> {
            Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            headers.putAll(exchange.getRequestHeaders());
            seenHeaders.set(headers);
            seenBody.set(exchange.getRequestBody().readAllBytes());

            byte[] response = OK_BODY.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private boolean sentUpgradeHandshake() {
        Map<String, List<String>> headers = seenHeaders.get();
        boolean upgradeHeader = headers.containsKey("Upgrade");
        boolean connectionUpgrade = headers.getOrDefault("Connection", List.of()).stream()
                .anyMatch(v -> v.toLowerCase().contains("upgrade"));
        return upgradeHeader || connectionUpgrade;
    }

    @Test
    void productionConfigSendsPlainHttp11WithTheFullBody() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        new EngineHttpConfig().http11RestClientCustomizer().customize(builder);

        EngineClient client = new EngineClient(builder, mapper, baseUrl(), "test-only-engine-key");
        EngineDtos.RecommendationResponse response = client.recommend(
                EngineDtosContractTest.demoTshirtRequest());

        assertThat(response.recommendedSize()).isEqualTo("M");

        // No HTTP/2 upgrade handshake (this is what made Uvicorn drop the body).
        assertThat(sentUpgradeHandshake()).isFalse();

        // The body really arrived, is non-empty, and matches Content-Length.
        byte[] body = seenBody.get();
        assertThat(body).isNotEmpty();
        assertThat(seenHeaders.get().get("Content-Length"))
                .containsExactly(String.valueOf(body.length));

        // And it is the JSON the engine expects.
        JsonNode json = mapper.readTree(body);
        assertThat(json.get("category").asText()).isEqualTo("T_SHIRT");
        assertThat(json.get("sizes").isArray()).isTrue();
    }

    /**
     * Control: proves this test can detect the bug, using the old default JDK
     * client.
     */
    @Test
    void controlDefaultJdkClientDoesSendTheUpgradeHandshake() {
        RestClient.Builder builder = RestClient.builder();
        builder.requestFactory(new JdkClientHttpRequestFactory());

        EngineClient client = new EngineClient(builder, mapper, baseUrl(), "test-only-engine-key");
        client.recommend(EngineDtosContractTest.demoTshirtRequest());

        assertThat(sentUpgradeHandshake()).isTrue();
    }
}