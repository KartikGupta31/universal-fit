
package com.universalfit.service;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import com.universalfit.integration.engine.EngineClient;
import com.universalfit.integration.engine.EngineDtos;
import com.universalfit.integration.engine.EngineUnavailableException;
import com.universalfit.integration.engine.EngineRejectedRequestException;
import com.universalfit.integration.engine.EngineResponseException;
import com.universalfit.repository.*;

import lombok.RequiredArgsConstructor;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserRepository userRepository;
    private final MeasurementProfileRepository measurementProfileRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductSizeSpecRepository productSizeSpecRepository;
    private final SizeChartRepository sizeChartRepository;
    private final FitRecommendationRepository fitRecommendationRepository;
    private final EngineClient engineClient;

    /**
     * Generate and persist a recommendation for an authenticated user.
     */
    public FitRecommendation recommend(String userEmail, ObjectId productId) {

        // 1. Resolve user and validate account status and consent.
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "User account is disabled");
        }

        if (!user.isMeasurementConsent()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User has not granted measurement consent");
        }

        // 2. Load measurement profile.
        MeasurementProfile profile =
                measurementProfileRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Measurement profile not found"));

        // 3. Load and validate product.
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));

        if (!product.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Product is not active");
        }

        // 4. Load and validate brand.
        Brand brand = brandRepository.findById(product.getBrandId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Brand not found"));

        if (!brand.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Brand is not active");
        }

        // 5. Select sizing source.
        // Product-specific measurements always take priority.
        SizingSource source;

        Optional<ProductSizeSpec> optionalSpec =
                productSizeSpecRepository.findByProductId(productId);

        if (optionalSpec.isPresent()
                && Boolean.FALSE.equals(optionalSpec.get().getRecommendationEligible())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "This product's source measurements are retained for reference but are not eligible for size recommendations yet.");
        }

        if (optionalSpec.isPresent()) {

            source = new SizingSource(optionalSpec.get(), null);

        } else {

            List<SizeChart> candidates =
                    sizeChartRepository.findByBrandIdAndCategoryAndActiveTrue(
                            product.getBrandId(),
                            product.getCategory()
                    );

            List<SizeChart> filtered = candidates.stream()
                    .filter(chart ->
                            chart.getSizingSystem() == product.getSizingSystem()
                                    && Objects.equals(
                                            chart.getRegion(),
                                            product.getRegion()
                                    )
                    )
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No matching size chart available for product");
            }

            if (filtered.size() > 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Multiple size charts match the product criteria");
            }

            source = new SizingSource(null, filtered.get(0));
        }

        // 6. Validate sizing source entries.
        List<SizeEntry> entries = source.getSizes();

        if (entries == null || entries.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Selected sizing source has no size entries");
        }

        // 7. Build engine request.
        EngineDtos.Profile engineProfile =
                buildEngineProfile(profile);

        List<EngineDtos.Size> engineSizes =
                buildEngineSizes(entries);

        if (engineSizes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Selected sizing source contains no usable sizes");
        }

        EngineDtos.RecommendRequest request =
                new EngineDtos.RecommendRequest(
                        engineProfile,
                        product.getCategory(),
                        product.getFitType(),
                        source.getMeasurementBasis(),
                        source.getUnit(),
                        source.isGarmentFlat(),
                        source.isChartVerified(),
                        brand.getName(),
                        engineSizes
                );

        // 8. Call recommendation engine.
        EngineDtos.RecommendationResponse response;

        try {
            response = engineClient.recommend(request);

        } catch (EngineUnavailableException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Recommendation engine unavailable");
        } catch (EngineRejectedRequestException ex) {
            // The engine rejected the measurements or sizing data (details are logged by EngineClient).
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Your saved measurements or this product's size data could not be processed. "
                            + "Please review your measurements and try again.");
        } catch (EngineResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Recommendation engine returned an invalid response");
        }

        if (response == null
                || !Double.isFinite(response.confidence())
                || response.confidence() < 0.0
                || response.confidence() > 1.0
                || response.fitType() == null
                || response.reason() == null
                || response.reason().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Recommendation engine returned an invalid response");
        }

        Set<String> allowedLabels = engineSizes.stream()
                .map(EngineDtos.Size::label)
                .collect(Collectors.toSet());
        if ((response.recommendedSize() != null
                && !allowedLabels.contains(response.recommendedSize()))
                || (response.alternatives() != null
                && response.alternatives().stream()
                .anyMatch(label -> label == null || !allowedLabels.contains(label)))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Recommendation engine returned an unknown size label");
        }

        // 9. Persist recommendation.
        FitRecommendation recommendation =
                FitRecommendation.builder()
                        .userId(user.getId())
                        .productId(product.getId())
                        .brandId(brand.getId())
                        .category(product.getCategory())

                        // Null when a product-specific spec was used.
                        .sizeChartId(source.getSizeChartId())

                        .recommendedSize(response.recommendedSize())
                        .confidence(response.confidence())
                        .uncertain(response.uncertain())
                        .fitType(response.fitType())
                        .reason(response.reason())

                        // Normalize nullable collections.
                        .alternatives(
                                response.alternatives() != null
                                        ? response.alternatives()
                                        : new ArrayList<>()
                        )

                        .measurementAnalysis(
                                response.measurementAnalysis() != null
                                        ? response.measurementAnalysis()
                                        : new LinkedHashMap<>()
                        )

                        .warnings(
                                response.warnings() != null
                                        ? response.warnings()
                                        : new ArrayList<>()
                        )

                        // Trust the selected source, not engine metadata.
                        .chartVerified(source.isChartVerified())

                        .build();

        return fitRecommendationRepository.save(recommendation);
    }


    public FitRecommendation recommendByBrandAndCategory(String userEmail, ObjectId brandId, Category category, String requestedRegion, SizingSystem requestedSizingSystem) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isEnabled()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is disabled");
        if (!user.isMeasurementConsent()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User has not granted measurement consent");
        MeasurementProfile profile = measurementProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Measurement profile not found"));
        Brand brand = brandRepository.findById(brandId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
        if (!brand.isActive()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brand is not active");
        if (brand.getCategories() == null || !brand.getCategories().contains(category))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This brand does not support the selected category");
        Map<Category, List<SizingSystem>> sizingAvailability = brand.getSizingSystemsByCategory();
        if (sizingAvailability != null && !sizingAvailability.isEmpty()
                && !sizingAvailability.getOrDefault(category, List.of()).contains(requestedSizingSystem))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No " + requestedSizingSystem + " size data is available for this brand and category.");

        List<Product> products = productRepository.findByActiveTrueAndBrandIdAndCategory(brandId, category).stream()
                .filter(p -> p.getSizingSystem() == requestedSizingSystem)
                .collect(Collectors.toList());
        List<SizeChart> charts = sizeChartRepository.findByBrandIdAndCategoryAndActiveTrue(brandId, category);
        Set<String> regions = new LinkedHashSet<>();
        products.stream().map(Product::getRegion).filter(r -> r != null && !r.isBlank()).forEach(regions::add);
        charts.stream().map(SizeChart::getRegion).filter(r -> r != null && !r.isBlank()).forEach(regions::add);
        String region = requestedRegion == null || requestedRegion.isBlank() ? null : requestedRegion.trim().toUpperCase(Locale.ROOT);
        if (region == null && regions.size() > 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Choose a sizing region: " + String.join(", ", regions));
        if (region == null && regions.size() == 1) region = regions.iterator().next();
        if (region != null && !regions.isEmpty() && !regions.contains(region))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No sizing data is available for region " + region);

        final String selectedRegion = region;
        List<Product> regionalProducts = products.stream()
                .filter(x -> selectedRegion == null || Objects.equals(x.getRegion(), selectedRegion)).collect(Collectors.toList());
        List<Product> withSpecs = regionalProducts.stream()
                .filter(x -> productSizeSpecRepository.findByProductId(x.getId()).isPresent()).collect(Collectors.toList());
        if (withSpecs.size() == 1) return recommend(userEmail, withSpecs.get(0).getId());
        if (withSpecs.size() > 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple sizing references match this brand and category.");

        List<SizeChart> matches = charts.stream()
                .filter(x -> x.getSizingSystem() == requestedSizingSystem)
                .filter(x -> selectedRegion == null || Objects.equals(x.getRegion(), selectedRegion))
                .collect(Collectors.toList());
        if (matches.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No size chart is available for this brand and category.");
        if (matches.size() > 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple size charts match this brand, category and region. The chart needs administrator clarification.");
        SizeChart chart = matches.get(0);
        List<EngineDtos.Size> engineSizes = buildEngineSizes(chart.getSizes());
        if (engineSizes.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "The selected size chart contains no usable sizes.");

        EngineDtos.RecommendRequest request = new EngineDtos.RecommendRequest(
                buildEngineProfile(profile), category, chart.getFitType(), chart.getMeasurementBasis(),
                chart.getUnit(), chart.isGarmentFlat(), chart.getVerificationStatus() == VerificationStatus.VERIFIED,
                brand.getName(), engineSizes);
        EngineDtos.RecommendationResponse response;
        try {
            response = engineClient.recommend(request);
        } catch (EngineUnavailableException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Recommendation engine unavailable");
        } catch (EngineRejectedRequestException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Your saved measurements or this size chart could not be processed. Please review your measurements and try again.");
        } catch (EngineResponseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Recommendation engine returned an invalid response");
        }
        if (response == null || !Double.isFinite(response.confidence()) || response.confidence() < 0 || response.confidence() > 1
                || response.fitType() == null || response.reason() == null || response.reason().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Recommendation engine returned an invalid response");
        Set<String> allowed = engineSizes.stream().map(EngineDtos.Size::label).collect(Collectors.toSet());
        if ((response.recommendedSize() != null && !allowed.contains(response.recommendedSize()))
                || (response.alternatives() != null && response.alternatives().stream().anyMatch(x -> x == null || !allowed.contains(x))))
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Recommendation engine returned an unknown size label");

        FitRecommendation result = FitRecommendation.builder().userId(user.getId()).brandId(brand.getId())
                .category(category).sizeChartId(chart.getId()).recommendedSize(response.recommendedSize())
                .confidence(response.confidence()).uncertain(response.uncertain()).fitType(response.fitType())
                .reason(response.reason()).alternatives(response.alternatives() == null ? new ArrayList<>() : response.alternatives())
                .measurementAnalysis(response.measurementAnalysis() == null ? new LinkedHashMap<>() : response.measurementAnalysis())
                .warnings(response.warnings() == null ? new ArrayList<>() : response.warnings())
                .chartVerified(chart.getVerificationStatus() == VerificationStatus.VERIFIED).build();
        return fitRecommendationRepository.save(result);
    }

    /**
     * Convert stored measurement profile into engine profile.
     */
    private EngineDtos.Profile buildEngineProfile(
            MeasurementProfile mp) {

        Double weight = mp.getWeightKg();

        // Stored weight is always in kilograms.
        // Engine expects pounds when profile unit is IN.
        if (mp.getUnit() == Unit.IN && weight != null) {
            weight = weight / 0.45359237;
        }

        Map<String, Double> values = mp.getValues();

        return new EngineDtos.Profile(
                mp.getUnit(),
                mp.getPreferredFit(),
                getValue(values, "height"),
                weight,
                getValue(values, "chest"),
                getValue(values, "waist"),
                getValue(values, "hips"),
                getValue(values, "shoulder"),
                getValue(values, "neck"),
                getValue(values, "sleeve"),
                getValue(values, "inseam"),
                getValue(values, "thigh"),
                getValue(values, "footLength")
        );
    }

    /**
     * Safely retrieve an optional measurement.
     */
    private Double getValue(
            Map<String, Double> map,
            String key) {

        if (map == null) {
            return null;
        }

        return map.get(key);
    }

    /**
     * Convert size entries into engine DTOs.
     */
    private List<EngineDtos.Size> buildEngineSizes(
            List<SizeEntry> entries) {

        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        return entries.stream()
                .filter(Objects::nonNull)
                .map(entry -> new EngineDtos.Size(
                        entry.getLabel(),
                        mapMeasurements(entry.getMeasurements())
                ))
                .filter(size ->
                        size.label() != null
                                && !size.label().isBlank()
                                && !size.measurements().isEmpty()
                )
                .collect(Collectors.toList());
    }

    /**
     * Convert measurement ranges safely.
     */
    private Map<String, EngineDtos.Measurement> mapMeasurements(
            Map<String, MeasurementRange> source) {

        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, EngineDtos.Measurement> result =
                new LinkedHashMap<>();

        source.forEach((key, range) -> {

            if (key != null && !key.isBlank() && range != null) {

                result.put(
                        key,
                        new EngineDtos.Measurement(
                                range.getMin(),
                                range.getMax()
                        )
                );
            }
        });

        return result;
    }

    /**
     * Holds the selected sizing source.
     */
    private static class SizingSource {

        private final ProductSizeSpec productSizeSpec;
        private final SizeChart sizeChart;

        SizingSource(
                ProductSizeSpec productSizeSpec,
                SizeChart sizeChart) {

            this.productSizeSpec = productSizeSpec;
            this.sizeChart = sizeChart;
        }

        List<SizeEntry> getSizes() {

            if (productSizeSpec != null) {
                return productSizeSpec.getSizes();
            }

            if (sizeChart != null) {
                return sizeChart.getSizes();
            }

            return Collections.emptyList();
        }

        Unit getUnit() {

            return productSizeSpec != null
                    ? productSizeSpec.getUnit()
                    : sizeChart.getUnit();
        }

        MeasurementBasis getMeasurementBasis() {

            return productSizeSpec != null
                    ? productSizeSpec.getMeasurementBasis()
                    : sizeChart.getMeasurementBasis();
        }

        boolean isGarmentFlat() {

            return productSizeSpec != null
                    ? productSizeSpec.isGarmentFlat()
                    : sizeChart.isGarmentFlat();
        }

        boolean isChartVerified() {

            VerificationStatus status =
                    productSizeSpec != null
                            ? productSizeSpec.getVerificationStatus()
                            : sizeChart.getVerificationStatus();

            return status == VerificationStatus.VERIFIED;
        }

        ObjectId getSizeChartId() {

            return sizeChart != null
                    ? sizeChart.getId()
                    : null;
        }
    }
}
