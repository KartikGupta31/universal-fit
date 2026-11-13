package com.universalfit.controller;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import com.universalfit.dto.BrandResponse;
import com.universalfit.dto.SizeChartResponse;
import com.universalfit.repository.BrandRepository;
import com.universalfit.repository.ProductRepository;
import com.universalfit.repository.ProductSizeSpecRepository;
import com.universalfit.repository.SizeChartRepository;
import com.universalfit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

/**
 * Admin-only maintenance endpoints for brand metadata and size charts.
 * Newly created/edited charts are always UNVERIFIED until explicitly reviewed.
 */
@RestController
@RequestMapping("/api/admin/data")
@RequiredArgsConstructor
public class AdminDataController {
    private final BrandRepository brandRepository;
    private final SizeChartRepository chartRepository;
    private final ProductRepository productRepository;
    private final ProductSizeSpecRepository productSizeSpecRepository;
    private final UserRepository userRepository;

    @GetMapping("/brands")
    public List<BrandResponse> brands(Authentication auth) {
        requireAdmin(auth);
        return brandRepository.findAll().stream()
                .sorted(Comparator.comparing(Brand::getName, String.CASE_INSENSITIVE_ORDER))
                .map(BrandResponse::from).toList();
    }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    public BrandResponse createBrand(@RequestBody BrandRequest request, Authentication auth) {
        User admin = requireAdmin(auth);
        String name = required(request.name(), "Brand name");
        String slug = slug(request.slug() == null || request.slug().isBlank() ? name : request.slug());
        if (brandRepository.existsBySlug(slug))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A brand with this slug already exists");
        Brand brand = Brand.builder().slug(slug).name(name).logoUrl(clean(request.logoUrl()))
                .country(clean(request.country())).supportedRegions(cleanList(request.supportedRegions()))
                .categories(new ArrayList<>()).sizingSystemsByCategory(new EnumMap<>(Category.class))
                .dataSource(clean(request.dataSource())).notes(clean(request.notes()))
                .verificationStatus(VerificationStatus.UNVERIFIED).active(true).build();
        return BrandResponse.from(brandRepository.save(brand));
    }

    @PutMapping("/brands/{brandId}")
    public BrandResponse updateBrand(@PathVariable String brandId, @RequestBody BrandRequest request,
                                     Authentication auth) {
        requireAdmin(auth);
        Brand brand = findBrand(brandId);
        String name = required(request.name(), "Brand name");
        String slug = slug(request.slug() == null || request.slug().isBlank() ? name : request.slug());
        if (!slug.equals(brand.getSlug()) && brandRepository.existsBySlug(slug))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A brand with this slug already exists");
        brand.setName(name); brand.setSlug(slug); brand.setLogoUrl(clean(request.logoUrl()));
        brand.setCountry(clean(request.country())); brand.setSupportedRegions(cleanList(request.supportedRegions()));
        brand.setDataSource(clean(request.dataSource())); brand.setNotes(clean(request.notes()));
        return BrandResponse.from(brandRepository.save(brand));
    }

    @PatchMapping("/brands/{brandId}/active")
    public BrandResponse setBrandActive(@PathVariable String brandId, @RequestBody ActiveRequest request,
                                        Authentication auth) {
        requireAdmin(auth);
        if (request == null || request.active() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "active is required");
        Brand brand = findBrand(brandId);
        brand.setActive(request.active());
        return BrandResponse.from(brandRepository.save(brand));
    }

    @GetMapping("/charts")
    public List<AdminChartItem> charts(@RequestParam(required = false) String brandId,
                                       Authentication auth) {
        requireAdmin(auth);
        List<AdminChartItem> result = new ArrayList<>();
        List<SizeChart> charts = (brandId == null || brandId.isBlank())
                ? chartRepository.findAll()
                : chartRepository.findByBrandId(new ObjectId(validId(brandId, "brand")));
        charts.forEach(chart -> result.add(AdminChartItem.from(chart)));

        List<Product> products = (brandId == null || brandId.isBlank())
                ? productRepository.findAll()
                : productRepository.findByBrandId(new ObjectId(validId(brandId, "brand")));
        for (Product product : products) {
            productSizeSpecRepository.findByProductId(product.getId()).ifPresent(spec ->
                    result.add(AdminChartItem.from(product, spec)));
        }
        result.sort(Comparator.comparing(AdminChartItem::updatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @PostMapping("/charts")
    @ResponseStatus(HttpStatus.CREATED)
    public SizeChartResponse createChart(@RequestBody ChartRequest request, Authentication auth) {
        requireAdmin(auth);
        Brand brand = findBrand(request.brandId());
        SizeChart chart = buildChart(new SizeChart(), request, brand);
        chart.setVerificationStatus(VerificationStatus.UNVERIFIED);
        chart.setLastVerifiedAt(null); chart.setVerifiedBy(null);
        return SizeChartResponse.from(chartRepository.save(chart));
    }

    @PutMapping("/charts/{chartId}")
    public SizeChartResponse updateChart(@PathVariable String chartId, @RequestBody ChartRequest request,
                                         Authentication auth) {
        requireAdmin(auth);
        SizeChart chart = chartRepository.findById(new ObjectId(validId(chartId, "chart")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Size chart not found"));
        Brand brand = findBrand(request.brandId());
        buildChart(chart, request, brand);
        chart.setVerificationStatus(VerificationStatus.UNVERIFIED);
        chart.setLastVerifiedAt(null); chart.setVerifiedBy(null);
        return SizeChartResponse.from(chartRepository.save(chart));
    }

    @PatchMapping("/charts/{chartId}/verification")
    public SizeChartResponse verifyChart(@PathVariable String chartId, @RequestBody VerifyRequest request,
                                         Authentication auth) {
        User admin = requireAdmin(auth);
        SizeChart chart = chartRepository.findById(new ObjectId(validId(chartId, "chart")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Size chart not found"));
        if (request == null || request.verified() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verified is required");
        if (request.verified()) {
            if (chart.getSourceUrl() == null || chart.getSourceUrl().isBlank()
                    || chart.getDataSource() == null || chart.getDataSource().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A source name and source URL are required before verification");
            if (chart.getSizes() == null || chart.getSizes().isEmpty()
                    || chart.getSizes().stream().anyMatch(s -> s.getLabel() == null || s.getLabel().isBlank()
                    || s.getMeasurements() == null || s.getMeasurements().isEmpty()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every size needs a label and measurements");
            chart.setVerificationStatus(VerificationStatus.VERIFIED);
            chart.setLastVerifiedAt(Instant.now());
            chart.setVerifiedBy(admin.getId());
        } else {
            chart.setVerificationStatus(VerificationStatus.UNVERIFIED);
            chart.setLastVerifiedAt(null); chart.setVerifiedBy(null);
        }
        return SizeChartResponse.from(chartRepository.save(chart));
    }

    @PatchMapping("/charts/{chartId}/active")
    public SizeChartResponse setChartActive(@PathVariable String chartId, @RequestBody ActiveRequest request,
                                             Authentication auth) {
        requireAdmin(auth);
        if (request == null || request.active() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "active is required");
        SizeChart chart = chartRepository.findById(new ObjectId(validId(chartId, "chart")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Size chart not found"));
        chart.setActive(request.active());
        return SizeChartResponse.from(chartRepository.save(chart));
    }

    @PatchMapping("/product-specs/{specId}/verification")
    public AdminChartItem verifyProductSpec(@PathVariable String specId, @RequestBody VerifyRequest request,
                                            Authentication auth) {
        requireAdmin(auth);
        if (request == null || request.verified() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verified is required");
        ProductSizeSpec spec = productSizeSpecRepository.findById(new ObjectId(validId(specId, "product size spec")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product size specification not found"));
        Product product = productRepository.findById(spec.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (request.verified()) {
            if (spec.getSourceUrl() == null || spec.getSourceUrl().isBlank()
                    || spec.getDataSource() == null || spec.getDataSource().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A source name and source URL are required before verification");
            if (spec.getSizes() == null || spec.getSizes().isEmpty()
                    || spec.getSizes().stream().anyMatch(s -> s.getLabel() == null || s.getLabel().isBlank()
                    || s.getMeasurements() == null || s.getMeasurements().isEmpty()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every size needs a label and measurements");
            spec.setVerificationStatus(VerificationStatus.VERIFIED);
            spec.setLastVerifiedAt(Instant.now());
            product.setVerificationStatus(VerificationStatus.VERIFIED);
        } else {
            spec.setVerificationStatus(VerificationStatus.UNVERIFIED);
            spec.setLastVerifiedAt(null);
            product.setVerificationStatus(VerificationStatus.UNVERIFIED);
        }
        productRepository.save(product);
        return AdminChartItem.from(product, productSizeSpecRepository.save(spec));
    }

    @PatchMapping("/product-specs/{specId}/active")
    public AdminChartItem setProductSpecActive(@PathVariable String specId, @RequestBody ActiveRequest request,
                                               Authentication auth) {
        requireAdmin(auth);
        if (request == null || request.active() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "active is required");
        ProductSizeSpec spec = productSizeSpecRepository.findById(new ObjectId(validId(specId, "product size spec")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product size specification not found"));
        Product product = productRepository.findById(spec.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        product.setActive(request.active());
        productRepository.save(product);
        return AdminChartItem.from(product, spec);
    }

    private SizeChart buildChart(SizeChart chart, ChartRequest r, Brand brand) {
        if (r == null || r.category() == null || r.sizingSystem() == null || r.measurementBasis() == null
                || r.unit() == null || r.region() == null || r.region().isBlank()
                || r.sizes() == null || r.sizes().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category, sizing system, basis, unit, region and size rows are required");
        if (r.sizes().stream().anyMatch(s -> s == null || s.getLabel() == null || s.getLabel().isBlank()
                || s.getMeasurements() == null || s.getMeasurements().isEmpty()
                || s.getMeasurements().values().stream().anyMatch(v -> v == null || !Double.isFinite(v.getMin())
                || !Double.isFinite(v.getMax()) || v.getMin() < 0 || v.getMax() < v.getMin())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each size needs a label and valid measurement ranges");
        chart.setBrandId(brand.getId()); chart.setCategory(r.category()); chart.setSizingSystem(r.sizingSystem());
        chart.setRegion(r.region().trim().toUpperCase(Locale.ROOT)); chart.setCollection(clean(r.collection()));
        chart.setMeasurementBasis(r.measurementBasis()); chart.setUnit(r.unit());
        chart.setGarmentFlat(Boolean.TRUE.equals(r.garmentFlat()));
        chart.setFitType(r.fitType() == null ? FitPreference.REGULAR : r.fitType());
        chart.setSizes(r.sizes()); chart.setDataSource(clean(r.dataSource())); chart.setSourceUrl(clean(r.sourceUrl()));
        // Keep the public brand directory in sync with the chart records administrators create.
        Set<Category> categories = new LinkedHashSet<>(brand.getCategories() == null ? List.of() : brand.getCategories());
        categories.add(r.category());
        brand.setCategories(new ArrayList<>(categories));
        Map<Category, List<SizingSystem>> availability = new EnumMap<>(Category.class);
        if (brand.getSizingSystemsByCategory() != null) availability.putAll(brand.getSizingSystemsByCategory());
        Set<SizingSystem> systems = new LinkedHashSet<>(availability.getOrDefault(r.category(), List.of()));
        systems.add(r.sizingSystem());
        availability.put(r.category(), new ArrayList<>(systems));
        brand.setSizingSystemsByCategory(availability);
        brandRepository.save(brand);
        return chart;
    }

    private static String hex(ObjectId id) { return id == null ? null : id.toHexString(); }

    private Brand findBrand(String id) {
        return brandRepository.findById(new ObjectId(validId(id, "brand")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
    }
    private String validId(String id, String kind) {
        if (!ObjectId.isValid(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + kind + " ID");
        return id;
    }
    private User requireAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isEnabled() || user.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
        return user;
    }
    private String required(String value, String label) {
        if (value == null || value.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        return value.trim();
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private List<String> cleanList(List<String> values) {
        return values == null ? new ArrayList<>() : values.stream().filter(Objects::nonNull)
                .map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
    }
    private String slug(String value) {
        return value.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }


    /** Unified read model for brand-level charts and imported product-specific size specifications. */
    public record AdminChartItem(String id, String brandId, Category category, SizingSystem sizingSystem,
                                 String region, String productName, String collection,
                                 MeasurementBasis measurementBasis, Unit unit, boolean garmentFlat,
                                 FitPreference fitType, List<SizeEntry> sizes, String dataSource,
                                 String sourceUrl, VerificationStatus verificationStatus,
                                 Instant lastVerifiedAt, Instant updatedAt, boolean active,
                                 String sourceType, Boolean recommendationEligible, String measurementNotes) {
        static AdminChartItem from(SizeChart chart) {
            return new AdminChartItem(Ids.hex(chart.getId()), Ids.hex(chart.getBrandId()),
                    chart.getCategory(), chart.getSizingSystem(), chart.getRegion(), null,
                    chart.getCollection(), chart.getMeasurementBasis(), chart.getUnit(),
                    chart.isGarmentFlat(), chart.getFitType(), chart.getSizes(), chart.getDataSource(),
                    chart.getSourceUrl(), chart.getVerificationStatus(), chart.getLastVerifiedAt(),
                    chart.getUpdatedAt(), chart.isActive(), "BRAND_CHART", true, null);
        }
        static AdminChartItem from(Product product, ProductSizeSpec spec) {
            return new AdminChartItem(Ids.hex(spec.getId()), Ids.hex(product.getBrandId()),
                    product.getCategory(), product.getSizingSystem(), product.getRegion(), product.getName(),
                    null, spec.getMeasurementBasis(), spec.getUnit(), spec.isGarmentFlat(),
                    product.getFitType(), spec.getSizes(), spec.getDataSource(), spec.getSourceUrl(),
                    spec.getVerificationStatus(), spec.getLastVerifiedAt(), spec.getUpdatedAt(),
                    product.isActive(), "PRODUCT_SPEC", spec.getRecommendationEligible(), spec.getMeasurementNotes());
        }
    }

    public record BrandRequest(String name, String slug, String logoUrl, String country,
                               List<String> supportedRegions, String dataSource, String notes) {}
    public record ChartRequest(String brandId, Category category, SizingSystem sizingSystem, String region,
                               String collection, MeasurementBasis measurementBasis, Unit unit,
                               Boolean garmentFlat, FitPreference fitType, List<SizeEntry> sizes,
                               String dataSource, String sourceUrl) {}
    public record ActiveRequest(Boolean active) {}
    public record VerifyRequest(Boolean verified) {}
}
