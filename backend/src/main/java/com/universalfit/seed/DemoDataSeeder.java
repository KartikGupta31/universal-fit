package com.universalfit.seed;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import com.universalfit.repository.*;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds FICTIONAL demo brands, size charts and products so the app can be tried end to end.
 *
 * Every record is {@link VerificationStatus#DEMO_UNVERIFIED}. The brand names are invented and the numbers are
 * placeholders chosen only to exercise the recommendation engine. They are NOT real brand sizing data.
 * Real brands and verified charts are added by an administrator with a documented source.
 *
 * Enabled with SEED_DEMO_DATA=true. Idempotent: running twice does not duplicate anything.
 */
@Component
@ConditionalOnProperty(name = "universalfit.seed.demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_SOURCE = "DEMO / UNVERIFIED - fictional placeholder data, not a real brand chart";
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final BrandRepository brands;
    private final SizeChartRepository charts;
    private final ProductRepository products;
    private final ProductSizeSpecRepository specs;

    public DemoDataSeeder(BrandRepository brands, SizeChartRepository charts,
                          ProductRepository products, ProductSizeSpecRepository specs) {
        this.brands = brands;
        this.charts = charts;
        this.products = products;
        this.specs = specs;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    /** Returns true if anything was created. */
    public boolean seed() {
        boolean created = false;
        created |= seedBrand("demo-alpha-wear", "DEMO Alpha Wear", "Demo Country",
                List.of("IN", "EU"), 0);
        created |= seedBrand("demo-beta-denim", "DEMO Beta Denim", "Demo Country",
                List.of("IN"), 6);
        if (created) {
            log.warn("Seeded DEMO / UNVERIFIED data. Do not use in production.");
        }
        return created;
    }

    private boolean seedBrand(String slug, String name, String country, List<String> regions, int offsetCm) {
        if (brands.existsBySlug(slug)) {
            return false;
        }
        Brand brand = brands.save(Brand.builder()
                .slug(slug).name(name).country(country).supportedRegions(regions)
                .categories(List.of(Category.T_SHIRT, Category.JEANS))
                .dataSource(DEMO_SOURCE)
                .verificationStatus(VerificationStatus.DEMO_UNVERIFIED)
                .notes("DEMO / UNVERIFIED. Fictional brand for development only.")
                .build());

        SizeChart teeBody = charts.save(chart(brand, Category.T_SHIRT, MeasurementBasis.BODY, false,
                bodySizes(offsetCm)));
        charts.save(chart(brand, Category.T_SHIRT, MeasurementBasis.GARMENT, false, garmentSizes(offsetCm)));
        charts.save(chart(brand, Category.JEANS, MeasurementBasis.GARMENT, false, jeansSizes(offsetCm)));

        Product tee = products.save(Product.builder()
                .name(name + " Everyday Tee (DEMO)").brandId(brand.getId()).category(Category.T_SHIRT)
                .sizingSystem(SizingSystem.UNISEX).region(regions.get(0))
                .description("DEMO / UNVERIFIED product used for development.")
                .availableSizes(List.of("S", "M", "L", "XL")).fitType(FitPreference.REGULAR)
                .dataSource(DEMO_SOURCE).verificationStatus(VerificationStatus.DEMO_UNVERIFIED).build());
        specs.save(ProductSizeSpec.builder().productId(tee.getId())
                .measurementBasis(MeasurementBasis.GARMENT).sizes(garmentSizes(offsetCm + 2))
                .dataSource(DEMO_SOURCE).verificationStatus(VerificationStatus.DEMO_UNVERIFIED).build());
        log.debug("Seeded demo brand {} (chart {})", slug, teeBody.getId());
        return true;
    }

    private SizeChart chart(Brand brand, Category category, MeasurementBasis basis, boolean flat, List<SizeEntry> sizes) {
        return SizeChart.builder()
                .brandId(brand.getId()).category(category).sizingSystem(SizingSystem.UNISEX)
                .region(brand.getSupportedRegions().get(0)).measurementBasis(basis).unit(Unit.CM)
                .garmentFlat(flat).sizes(sizes).dataSource(DEMO_SOURCE)
                .verificationStatus(VerificationStatus.DEMO_UNVERIFIED).lastVerifiedAt(null).build();
    }

    private static SizeEntry size(String label, Map<String, MeasurementRange> m) {
        return new SizeEntry(label, new LinkedHashMap<>(m));
    }

    private static List<SizeEntry> bodySizes(int off) {
        String[] labels = {"S", "M", "L", "XL"};
        List<SizeEntry> out = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            double chest = 88 + i * 8 + off;
            double waist = 74 + i * 8 + off;
            out.add(size(labels[i], Map.of(
                    "chest", MeasurementRange.of(chest, chest + 7),
                    "waist", MeasurementRange.of(waist, waist + 7))));
        }
        return out;
    }

    private static List<SizeEntry> garmentSizes(int off) {
        String[] labels = {"S", "M", "L", "XL"};
        List<SizeEntry> out = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            out.add(size(labels[i], Map.of(
                    "chest", MeasurementRange.of(98 + i * 6 + off),
                    "waist", MeasurementRange.of(94 + i * 6 + off),
                    "shoulder", MeasurementRange.of(43 + i * 2))));
        }
        return out;
    }

    private static List<SizeEntry> jeansSizes(int off) {
        String[] labels = {"30", "32", "34"};
        List<SizeEntry> out = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            out.add(size(labels[i], Map.of(
                    "waist", MeasurementRange.of(80 + i * 5 + off),
                    "hips", MeasurementRange.of(104 + i * 5 + off),
                    "inseam", MeasurementRange.of(80 + i))));
        }
        return out;
    }
}
