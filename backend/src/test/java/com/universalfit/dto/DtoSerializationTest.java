package com.universalfit.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Plain unit test (no Spring, no MongoDB, no Docker): proves ObjectIds reach the client as 24-character
 * hex strings, never as {"timestamp": ..., "date": ...} objects, and that internal fields stay hidden.
 */
class DtoSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static void assertHexString(JsonNode node, String field) {
        JsonNode value = node.get(field);
        assertThat(value).as(field).isNotNull();
        assertThat(value.isTextual()).as(field + " must be a string").isTrue();
        assertThat(value.asText()).as(field).matches("[0-9a-f]{24}");
    }

    @Test
    void brandIdIsHexString() throws Exception {
        ObjectId id = new ObjectId();
        Brand brand = Brand.builder().id(id).slug("demo").name("DEMO Brand")
                .verificationStatus(VerificationStatus.DEMO_UNVERIFIED).build();

        JsonNode json = mapper.valueToTree(BrandResponse.from(brand));

        assertHexString(json, "id");
        assertThat(json.get("id").asText()).isEqualTo(id.toHexString());
        assertThat(json.get("verificationStatus").asText()).isEqualTo("DEMO_UNVERIFIED");
        assertThat(json.has("timestamp")).isFalse();
    }

    @Test
    void productIdsAreHexStrings() {
        Product product = Product.builder().id(new ObjectId()).brandId(new ObjectId())
                .name("DEMO Tee").category(Category.T_SHIRT).build();

        JsonNode json = mapper.valueToTree(ProductResponse.from(product));

        assertHexString(json, "id");
        assertHexString(json, "brandId");
        assertThat(json.get("category").asText()).isEqualTo("T_SHIRT");
    }

    @Test
    void sizeChartIdsAreHexStringsAndVerifierIsHidden() {
        SizeChart chart = SizeChart.builder().id(new ObjectId()).brandId(new ObjectId())
                .verifiedBy(new ObjectId()).category(Category.JEANS)
                .measurementBasis(MeasurementBasis.GARMENT).build();
        chart.getSizes().add(new SizeEntry("32", new java.util.LinkedHashMap<>(
                java.util.Map.of("waist", MeasurementRange.of(85)))));

        JsonNode json = mapper.valueToTree(SizeChartResponse.from(chart));

        assertHexString(json, "id");
        assertHexString(json, "brandId");
        assertThat(json.has("verifiedBy")).as("internal admin id must not be exposed").isFalse();
        assertThat(json.get("sizes").get(0).get("label").asText()).isEqualTo("32");
    }

    @Test
    void pageResponseHasStableShape() {
        Brand brand = Brand.builder().id(new ObjectId()).slug("demo").name("DEMO").build();
        var page = new PageImpl<>(List.of(brand), PageRequest.of(0, 20), 1);

        JsonNode json = mapper.valueToTree(PageResponse.from(page, BrandResponse::from));

        assertThat(json.get("totalElements").asLong()).isEqualTo(1);
        assertThat(json.get("page").asInt()).isZero();
        assertThat(json.get("content")).hasSize(1);
        assertHexString(json.get("content").get(0), "id");
        assertThat(json.has("pageable")).as("Spring Page internals must not leak").isFalse();
    }

    @Test
    void nullIdsAreHandled() {
        JsonNode json = mapper.valueToTree(BrandResponse.from(Brand.builder().name("x").build()));
        assertThat(json.get("id").isNull()).isTrue();
    }
}
