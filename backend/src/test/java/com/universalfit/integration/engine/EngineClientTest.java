
package com.universalfit.integration.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** No Spring context, no Docker: EngineClient against a mock HTTP server. */
class EngineClientTest {

        private static final String API_KEY = "test-only-engine-key";

        private static final String OK_BODY = """
                        {"recommendedSize":"M","confidence":0.8,"uncertain":false,"fitType":"REGULAR",
                         "productFitType":"REGULAR","reason":"ok","alternatives":["L"],
                         "measurementAnalysis":{"chest":"good"},"sizeScores":{"M":0.9},
                         "measurementsUsed":["chest"],"missingMeasurements":[],"warnings":[],"dataQuality":{}}
                        """;

        private static final String ENGINE_422 = """
                        {"detail":[{"loc":["body","profile"],"msg":"Value error, chest=40.0 cm is outside the plausible adult range","type":"value_error"}]}
                        """;

        private MockRestServiceServer server;
        private EngineClient client;
        private Logger clientLogger;
        private ListAppender<ILoggingEvent> logs;

        @BeforeEach
        void setUp() {
                RestClient.Builder builder = RestClient.builder();
                server = MockRestServiceServer.bindTo(builder).build();

                client = new EngineClient(
                                builder,
                                new ObjectMapper(),
                                "http://engine.test",
                                API_KEY);

                clientLogger = (Logger) LoggerFactory.getLogger(EngineClient.class);
                logs = new ListAppender<>();
                logs.start();
                clientLogger.addAppender(logs);
        }

        @AfterEach
        void tearDown() {
                clientLogger.detachAppender(logs);
        }

        private String loggedText() {
                return logs.list.stream()
                                .map(ILoggingEvent::getFormattedMessage)
                                .reduce("", (a, b) -> a + "\n" + b);
        }

        @Test
        void sendsCamelCaseJsonAndApiKeyAndParsesTheResponse() {
                server.expect(requestTo("http://engine.test/v1/recommend"))
                                .andExpect(method(HttpMethod.POST))
                                .andExpect(header("X-API-Key", API_KEY))
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.category").value("T_SHIRT"))
                                .andExpect(jsonPath("$.measurementBasis").value("GARMENT"))
                                .andExpect(jsonPath("$.productFitType").value("REGULAR"))
                                .andExpect(jsonPath("$.profile.preferredFit").value("REGULAR"))
                                .andExpect(jsonPath("$.sizes[0].label").value("S"))
                                .andExpect(jsonPath("$.sizes[0].measurements.chest.min").value(100.0))
                                .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

                EngineDtos.RecommendationResponse response = client
                                .recommend(EngineDtosContractTest.demoTshirtRequest());

                assertThat(response.recommendedSize()).isEqualTo("M");
                assertThat(response.confidence()).isEqualTo(0.8);
                assertThat(response.alternatives()).containsExactly("L");

                server.verify();
        }

        @Test
        void engine422BecomesRejectedRequestAndDoesNotLogSensitiveDetails() {
                server.expect(requestTo("http://engine.test/v1/recommend"))
                                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .body(ENGINE_422));

                assertThatThrownBy(() -> client.recommend(EngineDtosContractTest.demoTshirtRequest()))
                                .isInstanceOf(EngineRejectedRequestException.class)
                                .hasMessageNotContaining("chest")
                                .hasMessageNotContaining("40.0")
                                .hasMessageNotContaining(API_KEY);

                String logged = loggedText();

                assertThat(logged).contains("422");

                assertThat(logged)
                                .doesNotContain("chest")
                                .doesNotContain("40.0")
                                .doesNotContain("plausible")
                                .doesNotContain("value_error")
                                .doesNotContain(API_KEY);
        }

        @Test
        void engine500IsUnavailable() {
                server.expect(requestTo("http://engine.test/v1/recommend"))
                                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body("boom"));

                assertThatThrownBy(() -> client.recommend(EngineDtosContractTest.demoTshirtRequest()))
                                .isInstanceOf(EngineUnavailableException.class);

                assertThat(loggedText()).contains("500");
        }

        @Test
        void engine401IsAConfigurationErrorNotARejectedRequest() {
                server.expect(requestTo("http://engine.test/v1/recommend"))
                                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .body("{\"detail\":\"Invalid or missing API key\"}"));

                assertThatThrownBy(() -> client.recommend(EngineDtosContractTest.demoTshirtRequest()))
                                .isInstanceOf(EngineResponseException.class)
                                .isNotInstanceOf(EngineRejectedRequestException.class);
        }

        @Test
        void emptyApiKeyDoesNotSendTheHeader() {
                RestClient.Builder builder = RestClient.builder();

                MockRestServiceServer noKeyServer = MockRestServiceServer.bindTo(builder).build();

                EngineClient noKeyClient = new EngineClient(
                                builder,
                                new ObjectMapper(),
                                "http://engine.test",
                                "");

                noKeyServer.expect(requestTo("http://engine.test/v1/recommend"))
                                .andExpect(request -> assertThat(request.getHeaders().containsKey("X-API-Key"))
                                                .isFalse())
                                .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

                noKeyClient.recommend(EngineDtosContractTest.demoTshirtRequest());

                noKeyServer.verify();
        }
}
