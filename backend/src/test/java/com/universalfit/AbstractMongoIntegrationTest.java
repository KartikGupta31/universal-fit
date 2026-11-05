package com.universalfit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import com.universalfit.repository.MeasurementHistoryRepository;
import com.universalfit.repository.MeasurementProfileRepository;
import com.universalfit.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for every integration test that needs MongoDB.
 *
 * <p>SAFETY: these tests delete data. They therefore talk ONLY to a throwaway MongoDB started by
 * Testcontainers on a random port. Three layers protect your Docker Compose development database
 * (localhost:27017):
 * <ol>
 *   <li>the app is pointed at the container through a {@code @DynamicPropertySource};</li>
 *   <li>the "test" profile sets a deliberately unreachable fallback URI;</li>
 *   <li>{@link #refuseToRunAgainstAnythingButTheTestContainer()} runs before any cleanup and fails the
 *       test unless the live connection is exactly the container (host and port, compared
 *       case-insensitively) AND the database is exactly {@code universalfit_test}.</li>
 * </ol>
 */
@SpringBootTest(classes = UniversalFitApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractMongoIntegrationTest {

    /** One container shared by all integration test classes in this JVM; stopped automatically at exit. */
    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0.14");

    /** The only database name these tests may use. The development database is "universalfit". */
    private static final String TEST_DATABASE = "universalfit_test";

    static {
        MONGO.start();
    }

    @DynamicPropertySource
    static void pointAppAtTestContainer(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> MONGO.getReplicaSetUrl(TEST_DATABASE));
    }

    protected static final String PASSWORD = "Secret123!";
    protected static final ObjectMapper JSON = new ObjectMapper();

    @Autowired protected MockMvc mockMvc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected MeasurementProfileRepository profileRepository;
    @Autowired protected MeasurementHistoryRepository historyRepository;
    @Autowired protected MongoTemplate mongoTemplate;
    @Autowired private MongoConnectionDetails mongoConnectionDetails;

    /**
     * Runs before each test (and before any subclass @BeforeEach). The safety guard always runs first;
     * data is only deleted once it has passed.
     */
    @BeforeEach
    final void guardThenCleanTestDatabase() {   // final: a subclass must not be able to override or shadow it
        refuseToRunAgainstAnythingButTheTestContainer();
        // The ONLY place in the test suite that clears whole collections. Documents are removed but
        // indexes are kept. It sits behind the guard above, so it can only ever empty the test container.
        for (String collection : mongoTemplate.getCollectionNames()) {
            if (!collection.startsWith("system.")) {
                mongoTemplate.getCollection(collection).deleteMany(new Document());
            }
        }
    }

    private void refuseToRunAgainstAnythingButTheTestContainer() {
        ConnectionString connection = mongoConnectionDetails.getConnectionString();

        // Host names are case-insensitive, so compare them in lower case.
        String expectedHost = (MONGO.getHost() + ":" + MONGO.getFirstMappedPort()).toLowerCase(Locale.ROOT);
        List<String> hosts = connection.getHosts().stream().map(h -> h.toLowerCase(Locale.ROOT)).toList();

        assertThat(hosts)
                .as("SAFETY GUARD: integration tests must only use the Testcontainers MongoDB at %s", expectedHost)
                .containsExactly(expectedHost);
        assertThat(hosts)
                .as("SAFETY GUARD: never run against the development database port")
                .noneMatch(host -> host.endsWith(":27017"));

        // The development database is called "universalfit"; the test one must be exactly this name.
        assertThat(connection.getDatabase())
                .as("SAFETY GUARD: the configured connection must name the test database")
                .isEqualTo(TEST_DATABASE);
        assertThat(mongoTemplate.getDb().getName())
                .as("SAFETY GUARD: the database the application actually writes to must be the test database")
                .isEqualTo(TEST_DATABASE);
    }

    // ----- helpers ---------------------------------------------------------

    /** Registers a user through the real endpoint and returns the JSON with accessToken and refreshToken. */
    protected JsonNode register(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(body);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
