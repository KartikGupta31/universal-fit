package com.universalfit.integration.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universalfit.domain.enums.Category;
import com.universalfit.domain.enums.FitPreference;
import com.universalfit.domain.enums.MeasurementBasis;
import com.universalfit.domain.enums.Unit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Plain unit test (no Spring, no Docker). Pins the exact JSON the backend sends to FastAPI against the shared
 * fixture in contracts/. The Python side posts the same file in engine/tests/test_contract.py.
 */
class EngineDtosContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The seeded demo tee (DEMO Alpha Wear, product spec = garmentSizes(2)), CM, GARMENT. */
    static EngineDtos.RecommendRequest demoTshirtRequest() {
        List<EngineDtos.Size> sizes = new ArrayList<>();
        String[] labels = {"S", "M", "L", "XL"};
        for (int i = 0; i < labels.length; i++) {
            Map<String, EngineDtos.Measurement> m = new LinkedHashMap<>();
            m.put("chest", new EngineDtos.Measurement(98.0 + i * 6 + 2, 98.0 + i * 6 + 2));
            m.put("waist", new EngineDtos.Measurement(94.0 + i * 6 + 2, 94.0 + i * 6 + 2));
            m.put("shoulder", new EngineDtos.Measurement(43.0 + i * 2, 43.0 + i * 2));
            sizes.add(new EngineDtos.Size(labels[i], m));
        }
        EngineDtos.Profile profile = new EngineDtos.Profile(
                Unit.CM, FitPreference.REGULAR,
                175.0, 70.0, 96.0, 82.0, null, 44.0, null, null, null, null, null);
        return new EngineDtos.RecommendRequest(
                profile, Category.T_SHIRT, FitPreference.REGULAR, MeasurementBasis.GARMENT,
                Unit.CM, false, false, "DEMO Alpha Wear", sizes);
    }

    private static JsonNode fixture() throws IOException {
        for (String candidate : List.of("../contracts/recommend-request.demo-tshirt.json",
                "contracts/recommend-request.demo-tshirt.json")) {
            Path path = Path.of(candidate);
            if (Files.exists(path)) {
                return MAPPER.readTree(Files.readString(path));
            }
        }
        throw new IOException("contracts/recommend-request.demo-tshirt.json not found from "
                + Path.of("").toAbsolutePath());
    }

    @Test
    void serialisedRequestEqualsTheSharedContractFixture() throws Exception {
        JsonNode actual = MAPPER.valueToTree(demoTshirtRequest());
        assertThat(actual).isEqualTo(fixture());
    }

    @Test
    void everyKeyIsCamelCaseAndEnumsAreUppercaseNames() {
        JsonNode json = MAPPER.valueToTree(demoTshirtRequest());
        assertNoSnakeCaseKeys(json);
        assertThat(json.has("productFitType")).isTrue();
        assertThat(json.has("measurementBasis")).isTrue();
        assertThat(json.has("garmentFlat")).isTrue();
        assertThat(json.has("chartVerified")).isTrue();
        assertThat(json.has("brandName")).isTrue();
        assertThat(json.get("profile").has("preferredFit")).isTrue();
        assertThat(json.get("profile").has("footLength")).isTrue();
        assertThat(json.get("category").asText()).isEqualTo("T_SHIRT");
        assertThat(json.get("measurementBasis").asText()).isEqualTo("GARMENT");
        assertThat(json.get("unit").asText()).isEqualTo("CM");
    }

    @Test
    void absentProfileMeasurementsAreSerialisedAsNull() {
        JsonNode profile = MAPPER.valueToTree(demoTshirtRequest()).get("profile");
        assertThat(profile.has("hips")).isTrue();
        assertThat(profile.get("hips").isNull()).isTrue();
    }

    private static void assertNoSnakeCaseKeys(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                assertThat(field.getKey()).as("key %s", field.getKey()).doesNotContain("_");
                assertNoSnakeCaseKeys(field.getValue());
            }
        } else if (node.isArray()) {
            node.forEach(EngineDtosContractTest::assertNoSnakeCaseKeys);
        }
    }
}
