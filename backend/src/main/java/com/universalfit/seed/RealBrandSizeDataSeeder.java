package com.universalfit.seed;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import com.universalfit.repository.*;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Imports exact style-specific measurements from official Levi's India product pages.
 * These are GARMENT measurements for the named style only, not universal body charts.
 * Every source record is UNVERIFIED until an administrator reviews it.
 *
 * Enable with SEED_REAL_BRAND_DATA=true. Safe to run repeatedly; products are keyed by source URL.
 */
@Component
@ConditionalOnProperty(name = "universalfit.seed.real-brand-data", havingValue = "true")
public class RealBrandSizeDataSeeder implements ApplicationRunner {
    private static final String SOURCE = "Official Levi's India product size chart";
    private final BrandRepository brands;
    private final ProductRepository products;
    private final ProductSizeSpecRepository specs;

    public RealBrandSizeDataSeeder(BrandRepository brands, ProductRepository products,
                                   ProductSizeSpecRepository specs) {
        this.brands = brands;
        this.products = products;
        this.specs = specs;
    }

    @Override
    public void run(ApplicationArguments args) {
        // These brands are catalog entries. Categories are exposed only where source data exists.
        Brand zara = saveBrand("zara", "Zara", "https://www.zara.com/in/", List.of(),
                "Official India storefront. No reusable category chart has been imported.");
        Brand hm = saveBrand("h-and-m", "H&M",
                "https://www2.hm.com/en_in/customer-service/sizeguide/sizing-faq.html", List.of(),
                "India sizing guidance points shoppers to the size guide on individual product pages. No reusable category chart imported.");
        Brand uniqlo = saveBrand("uniqlo", "UNIQLO",
                "https://faq-in.uniqlo.com/pkb_Home_UQ_IN?fs=RelatedArticle&id=kA0Ie000000TP16&l=en_US",
                List.of(), "India sizing guidance is product-specific; no generic category chart imported.");

        Brand levis = saveBrand("levis", "Levi's", "https://levi.in/",
                List.of(Category.T_SHIRT, Category.SHIRT, Category.JEANS),
                "Only the exact linked India product styles have data. Trousers, jackets and dresses have not been sourced.");

        // Men's slim-fit T-shirt, official table values in centimetres.
        product(levis, "Men's Solid Slim Fit T-shirt", Category.T_SHIRT, SizingSystem.MEN,
                "https://levi.in/products/mens-solid-slim-fit-t-shirt-169601462", FitPreference.SLIM,
                List.of(
                    row("XS", 40.64, 91.44, 67.31), row("S", 43.18, 97.79, 69.22),
                    row("M", 45.72, 104.14, 71.12), row("L", 48.26, 110.49, 73.03),
                    row("XL", 50.80, 116.84, 74.93), row("XXL", 53.34, 123.19, 76.84)));

        // Women's relaxed-fit T-shirt, kept as an independent chart.
        product(levis, "Women's Solid Black Relaxed Fit T-shirt", Category.T_SHIRT, SizingSystem.WOMEN,
                "https://levi.in/products/womens-solid-black-relaxed-fit-t-shirt-005x00000", FitPreference.RELAXED,
                List.of(
                    row("XS", 40.64, 96.52, 57.15), row("S", 43.18, 101.60, 58.42),
                    row("M", 45.72, 106.68, 59.69), row("L", 48.90, 113.03, 60.96),
                    row("XL", 52.71, 120.65, 62.55), row("XXL", 56.52, 128.27, 64.14)));

        // Men's slim-fit shirt, official table values in centimetres.
        product(levis, "Men's Solid Slim Fit Shirt", Category.SHIRT, SizingSystem.MEN,
                "https://levi.in/products/mens-solid-slim-fit-shirt-329080300", FitPreference.SLIM,
                List.of(
                    row("XXS", 39.37, 88.90, 63.50), row("XS", 41.91, 95.25, 66.04),
                    row("S", 44.45, 101.60, 68.58), row("M", 46.99, 107.95, 71.12),
                    row("L", 48.90, 115.57, 73.66), row("XL", 50.80, 123.19, 76.20),
                    row("XXL", 53.34, 130.81, 78.74), row("XXXL", 55.88, 138.43, 81.28)));

        // Women's 501 Original Jeans. Source provides waist, thigh and leg-opening dimensions.
        product(levis, "Women's 501 Original Jeans", Category.JEANS, SizingSystem.WOMEN,
                "https://levi.in/products/levis-womens-501-original-jeans-125010640", FitPreference.REGULAR,
                List.of(
                    jeans("23",75.57,56.85,35.56), jeans("24",78.74,58.12,36.20),
                    jeans("25",81.28,59.39,36.83), jeans("26",84.46,60.96,37.47),
                    jeans("27",87.00,62.23,38.10), jeans("28",90.17,63.50,38.74),
                    jeans("29",92.71,64.77,39.37), jeans("30",95.89,66.04,40.01),
                    jeans("31",98.43,67.31,40.64), jeans("32",101.60,68.91,41.28),
                    jeans("33",104.14,70.18,41.91), jeans("34",107.32,71.44,42.55)));
    }

    private Brand saveBrand(String slug, String name, String officialUrl,
                            List<Category> categories, String notes) {
        Brand brand = brands.findBySlug(slug).orElseGet(Brand::new);
        brand.setSlug(slug);
        brand.setName(name);
        brand.setCountry("IN");
        brand.setSupportedRegions(List.of("IN"));
        brand.setCategories(new ArrayList<>(categories));
        brand.setDataSource(officialUrl);
        brand.setVerificationStatus(VerificationStatus.UNVERIFIED);
        brand.setLastVerifiedAt(null);
        brand.setNotes(notes);
        brand.setActive(true);
        return brands.save(brand);
    }

    private void product(Brand brand, String name, Category category, SizingSystem sizingSystem,
                         String sourceUrl, FitPreference fit, List<SizeEntry> sizes) {
        Product product = products.findByProductUrl(sourceUrl).orElseGet(Product::new);
        product.setName(name);
        product.setBrandId(brand.getId());
        product.setCategory(category);
        product.setSizingSystem(sizingSystem);
        product.setRegion("IN");
        product.setProductUrl(sourceUrl);
        product.setFitType(fit);
        product.setAvailableSizes(sizes.stream().map(SizeEntry::getLabel).toList());
        product.setDataSource(SOURCE);
        product.setVerificationStatus(VerificationStatus.UNVERIFIED);
        product.setActive(true);
        product = products.save(product);

        ProductSizeSpec spec = specs.findByProductId(product.getId()).orElseGet(ProductSizeSpec::new);
        spec.setProductId(product.getId());
        spec.setMeasurementBasis(MeasurementBasis.GARMENT);
        spec.setUnit(Unit.CM);
        spec.setGarmentFlat(false);
        spec.setSizes(sizes);
        spec.setDataSource(SOURCE);
        spec.setSourceUrl(sourceUrl);
        spec.setVerificationStatus(VerificationStatus.UNVERIFIED);
        spec.setLastVerifiedAt(null);
        specs.save(spec);
    }

    private static SizeEntry row(String label, double shoulder, double chest, double length) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("shoulder", MeasurementRange.of(shoulder));
        m.put("chest", MeasurementRange.of(chest));
        m.put("length", MeasurementRange.of(length));
        return new SizeEntry(label, m);
    }

    private static SizeEntry jeans(String label, double waist, double thigh, double legOpening) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("waist", MeasurementRange.of(waist));
        m.put("thigh", MeasurementRange.of(thigh));
        m.put("legOpening", MeasurementRange.of(legOpening));
        return new SizeEntry(label, m);
    }
}
