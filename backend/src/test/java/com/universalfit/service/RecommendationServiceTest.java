package com.universalfit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import com.universalfit.integration.engine.EngineClient;
import com.universalfit.integration.engine.EngineDtos;
import com.universalfit.integration.engine.EngineRejectedRequestException;
import com.universalfit.integration.engine.EngineResponseException;
import com.universalfit.integration.engine.EngineUnavailableException;
import com.universalfit.repository.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Plain Mockito unit test: no Spring, no MongoDB, no Docker. */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    private static final String EMAIL = "user@example.com";

    @Mock UserRepository users;
    @Mock MeasurementProfileRepository profiles;
    @Mock ProductRepository products;
    @Mock BrandRepository brands;
    @Mock ProductSizeSpecRepository specs;
    @Mock SizeChartRepository charts;
    @Mock FitRecommendationRepository recommendations;
    @Mock EngineClient engine;

    private RecommendationService service;
    private final ObjectId userId = new ObjectId();
    private final ObjectId productId = new ObjectId();
    private final ObjectId brandId = new ObjectId();

    @BeforeEach
    void setUp() {
        service = new RecommendationService(users, profiles, products, brands, specs, charts, recommendations, engine);
    }

    private void stubUser(boolean consent) {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(
                User.builder().id(userId).email(EMAIL).measurementConsent(consent).build()));
    }

    /** User with consent, a saved profile, an active demo-style T-shirt and a product-specific spec. */
    private void stubDemoTshirtHappyPath() {
        stubUser(true);
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("height", 175.0);
        values.put("chest", 96.0);
        values.put("waist", 82.0);
        values.put("shoulder", 44.0);
        when(profiles.findByUserId(userId)).thenReturn(Optional.of(MeasurementProfile.builder()
                .userId(userId).unit(Unit.CM).preferredFit(FitPreference.REGULAR)
                .values(values).weightKg(70.0).build()));
        when(products.findById(productId)).thenReturn(Optional.of(Product.builder()
                .id(productId).brandId(brandId).name("DEMO Tee").category(Category.T_SHIRT)
                .fitType(FitPreference.REGULAR).build()));
        when(brands.findById(brandId)).thenReturn(Optional.of(
                Brand.builder().id(brandId).name("DEMO Alpha Wear").build()));

        List<SizeEntry> sizes = List.of("S", "M", "L", "XL").stream().map(label -> {
            int i = List.of("S", "M", "L", "XL").indexOf(label);
            Map<String, MeasurementRange> m = new LinkedHashMap<>();
            m.put("chest", MeasurementRange.of(100 + i * 6));
            m.put("waist", MeasurementRange.of(96 + i * 6));
            m.put("shoulder", MeasurementRange.of(43 + i * 2));
            return new SizeEntry(label, m);
        }).toList();
        when(specs.findByProductId(productId)).thenReturn(Optional.of(ProductSizeSpec.builder()
                .productId(productId).measurementBasis(MeasurementBasis.GARMENT).unit(Unit.CM)
                .sizes(sizes).verificationStatus(VerificationStatus.DEMO_UNVERIFIED).build()));
    }

    private static EngineDtos.RecommendationResponse okResponse() {
        return new EngineDtos.RecommendationResponse("M", 0.82, false, FitPreference.REGULAR, FitPreference.REGULAR,
                "Chest fits well.", List.of("L"), Map.of("chest", "good"),
                Map.of("S", 0.4, "M", 0.9, "L", 0.7, "XL", 0.2),
                List.of("chest", "waist", "shoulder"), List.of(), List.of(), Map.of());
    }

    @Test
    void demoTshirtBuildsTheEngineRequestAndPersistsTheResult() {
        stubDemoTshirtHappyPath();
        when(engine.recommend(any())).thenReturn(okResponse());
        when(recommendations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FitRecommendation saved = service.recommend(EMAIL, productId);

        ArgumentCaptor<EngineDtos.RecommendRequest> sent = ArgumentCaptor.forClass(EngineDtos.RecommendRequest.class);
        verify(engine).recommend(sent.capture());
        EngineDtos.RecommendRequest request = sent.getValue();
        assertThat(request.category()).isEqualTo(Category.T_SHIRT);
        assertThat(request.productFitType()).isEqualTo(FitPreference.REGULAR);
        assertThat(request.measurementBasis()).isEqualTo(MeasurementBasis.GARMENT);
        assertThat(request.unit()).isEqualTo(Unit.CM);
        assertThat(request.brandName()).isEqualTo("DEMO Alpha Wear");
        assertThat(request.sizes()).extracting(EngineDtos.Size::label).containsExactly("S", "M", "L", "XL");
        assertThat(request.sizes().get(0).measurements().keySet()).containsExactlyInAnyOrder("chest", "waist", "shoulder");
        assertThat(request.profile().chest()).isEqualTo(96.0);
        assertThat(request.profile().weight()).isEqualTo(70.0);
        assertThat(request.profile().hips()).isNull();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getRecommendedSize()).isEqualTo("M");
        assertThat(saved.getSizeChartId()).isNull();   // product-specific spec was used
        verify(recommendations).save(any(FitRecommendation.class));
    }

    @Test
    void rawSourceDimensionsAreNotEligibleForRecommendations() {
        stubDemoTshirtHappyPath();
        when(specs.findByProductId(productId)).thenReturn(Optional.of(ProductSizeSpec.builder()
                .productId(productId).recommendationEligible(false).build()));

        assertThatThrownBy(() -> service.recommend(EMAIL, productId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verifyNoInteractions(engine);
        verify(recommendations, never()).save(any());
    }

    @Test
    void missingConsentIsForbiddenAndNeverReachesTheEngine() {
        stubUser(false);

        assertThatThrownBy(() -> service.recommend(EMAIL, productId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(engine);
        verify(recommendations, never()).save(any());
    }

    @Test
    void engineRejectionIs422WithAGenericMessageAndNothingIsSaved() {
        stubDemoTshirtHappyPath();
        when(engine.recommend(any())).thenThrow(new EngineRejectedRequestException("rejected"));

        assertThatThrownBy(() -> service.recommend(EMAIL, productId))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(ex.getReason()).contains("review your measurements")
                            .doesNotContain("pydantic").doesNotContain("detail").doesNotContain("loc");
                });
        verify(recommendations, never()).save(any());
    }

    @Test
    void engineDownIs503AndOtherEngineErrorsAre502() {
        stubDemoTshirtHappyPath();
        when(engine.recommend(any()))
                .thenThrow(new EngineUnavailableException("down"))
                .thenThrow(new EngineResponseException("bad key"));

        assertThatThrownBy(() -> service.recommend(EMAIL, productId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> service.recommend(EMAIL, productId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
        verify(recommendations, never()).save(any());
    }
}
