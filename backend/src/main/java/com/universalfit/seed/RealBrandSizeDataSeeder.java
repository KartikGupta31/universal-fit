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
        Brand zara = saveBrand("zara", "Zara",
                "https://www.zara.com/in/en/zara-timeless---contrast-collar-t-shirt-p06061534.html", List.of(),
                "Official India product pages direct customers to the size guide or to request measurements. Numeric size rows were not available in the accessible official source, so no values were invented.");
        Brand hm = saveBrand("h-and-m", "H&M",
                "https://www2.hm.com/en_in/customer-service/sizeguide/sizing-faq.html", List.of(),
                "Two official India T-shirt product tables are retained as raw Width and Length values. The source does not define width axis/flat-vs-circumference, so these records are inactive and excluded from recommendations until verified.");
        rawProduct(hm, "Men's Light green Loose Fit Washed T-shirt", Category.T_SHIRT, SizingSystem.MEN,
                "https://www2.hm.com/en_in/productpage.1275904004.html", FitPreference.RELAXED,
                List.of(widthRow("XS",100,66), widthRow("S",108,67), widthRow("M",116,68), widthRow("L",124,70)));
        rawProduct(hm, "Women's Beige/Koiya Raglan-sleeved T-shirt", Category.T_SHIRT, SizingSystem.WOMEN,
                "https://www2.hm.com/en_in/productpage.1308774001.html", FitPreference.REGULAR,
                List.of(widthRow("XXS",69,48), widthRow("XS",72,50), widthRow("S",80,53),
                        widthRow("M",88,55), widthRow("L",96,56), widthRow("XL",107,58)));
        Brand uniqlo = saveBrand("uniqlo", "UNIQLO",
                "https://faq-in.uniqlo.com/pkb_Home_UQ_IN?id=kA0Ie000000TNqU",
                List.of(Category.T_SHIRT, Category.TROUSERS),
                "India product-specific body-size charts imported for a unisex T-shirt, women's T-shirt and women's trousers. Source distinguishes body measurements from garment measurements.");

        // UNIQLO publishes explicit BODY SIZE tables on these India product pages.
        // Its help page distinguishes body measurements from product widths; these imported
        // rows are only the tables explicitly labelled Body Size.
        uniqlo.setSizingSystemsByCategory(Map.of(
                Category.T_SHIRT, List.of(SizingSystem.UNISEX, SizingSystem.WOMEN),
                Category.TROUSERS, List.of(SizingSystem.WOMEN)));
        brands.save(uniqlo);
        product(uniqlo, "Unisex Supima Cotton Crew Neck Short Sleeve T-Shirt",
                Category.T_SHIRT, SizingSystem.UNISEX,
                "https://www.uniqlo.com/in/en/products/E455365-000/00?colorDisplayCode=12&sizeDisplayCode=005",
                FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    bodyRow("S",80,88,68,76,null,null), bodyRow("M",88,96,76,84,null,null),
                    bodyRow("L",96,104,84,92,null,null), bodyRow("XL",104,112,92,100,null,null),
                    bodyRow("XXL",112,120,100,108,null,null), bodyRow("3XL",120,128,108,115,null,null)));
        product(uniqlo, "Women's Supima Cotton T-Shirt",
                Category.T_SHIRT, SizingSystem.WOMEN,
                "https://www.uniqlo.com/in/en/products/E474408-000/00?colorCode=COL11&sizeCode=SMA004",
                FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    bodyRow("XS",73,79,57,63,80,86), bodyRow("S",77,83,61,67,84,90),
                    bodyRow("M",81,87,65,71,88,94), bodyRow("L",85,91,69,75,92,98),
                    bodyRow("XL",91,97,75,81,98,104), bodyRow("XXL",97,103,81,87,104,110),
                    bodyRow("3XL",103,109,87,93,110,115)));
        product(uniqlo, "Women's Smart Wide Pants",
                Category.TROUSERS, SizingSystem.WOMEN,
                "https://www.uniqlo.com/in/en/products/E479292-000/00?colorCode=COL58&sizeCode=SMA003",
                FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    bodyRow("S",null,null,61,67,84,90), bodyRow("M",null,null,65,71,88,94),
                    bodyRow("L",null,null,69,75,92,98), bodyRow("XL",null,null,75,81,98,104),
                    bodyRow("XXL",null,null,81,87,104,110)));

        Brand levis = saveBrand("levis", "Levi's", "https://levi.in/",
                List.of(Category.T_SHIRT, Category.SHIRT, Category.JEANS, Category.TROUSERS, Category.JACKET),
                "Exact linked India product styles have style-specific charts for T-shirts, shirts, jeans, trousers and jackets. No numerical India dress chart was found. All imported values remain UNVERIFIED pending review.");
        levis.setSizingSystemsByCategory(Map.of(
                Category.T_SHIRT, List.of(SizingSystem.MEN, SizingSystem.WOMEN),
                Category.SHIRT, List.of(SizingSystem.MEN, SizingSystem.WOMEN),
                Category.JEANS, List.of(SizingSystem.MEN, SizingSystem.WOMEN),
                Category.TROUSERS, List.of(SizingSystem.MEN),
                Category.JACKET, List.of(SizingSystem.MEN, SizingSystem.WOMEN)));
        brands.save(levis);

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

        // Men's 501 Black Straight Fit Mid Rise Jeans.
        product(levis, "Men's 501 Black Straight Fit Mid Rise Jeans", Category.JEANS, SizingSystem.MEN,
                "https://levi.in/products/mens-501-black-regular-fit-jeans-798300026", FitPreference.REGULAR,
                List.of(
                    jeansWithHips("24",71.12,97.79,54.31), jeansWithHips("25",73.66,100.33,55.58),
                    jeansWithHips("26",77.47,103.84,57.15), jeansWithHips("27",80.01,106.37,58.42),
                    jeansWithHips("28",82.55,108.91,59.69), jeansWithHips("29",85.09,111.45,60.96),
                    jeansWithHips("30",88.90,114.94,62.56), jeansWithHips("31",91.44,117.48,63.83),
                    jeansWithHips("32",93.98,120.02,65.10), jeansWithHips("33",96.52,122.56,66.37),
                    jeansWithHips("34",99.70,126.06,67.95), jeansWithHips("35",102.24,128.60,69.22),
                    jeansWithHips("36",104.78,131.14,70.49), jeansWithHips("38",110.49,136.85,73.36),
                    jeansWithHips("40",116.21,142.56,76.20), jeansWithHips("42",121.92,148.27,79.04),
                    jeansWithHips("44",127.64,153.98,81.92)));

        // Women's solid comfort-fit shirt.
        product(levis, "Women's Solid Comfort Fit Shirt", Category.SHIRT, SizingSystem.WOMEN,
                "https://levi.in/products/womens-solid-comfort-fit-shirt-0027m0011", FitPreference.RELAXED,
                List.of(
                    row("XXS",44.45,106.68,63.50), row("XS",45.72,111.76,64.77),
                    row("S",46.99,116.84,66.04), row("M",48.26,121.92,67.31),
                    row("L",50.17,128.27,68.58), row("XL",52.07,135.89,68.96),
                    row("XXL",53.98,143.51,69.12), row("XXXL",55.88,151.13,69.53)));

        // Men's slim-fit trousers.
        product(levis, "Men's Slim Fit Trousers", Category.TROUSERS, SizingSystem.MEN,
                "https://levi.in/products/mens-slim-fit-trousers-a40690000", FitPreference.SLIM,
                List.of(
                    bottom("XXS",66.04,99.72,57.50,29.85), bottom("XS",71.12,104.78,59.99,31.75),
                    bottom("S",76.20,109.96,62.55,33.66), bottom("M",83.82,117.48,66.36,35.56),
                    bottom("L",91.44,125.20,70.17,37.47), bottom("XL",99.06,132.72,74.05,39.37),
                    bottom("XXL",109.22,140.97,79.08,41.91), bottom("XXXL",119.38,149.22,84.16,44.45)));

        // Men's solid black jacket.
        product(levis, "Men's Solid Black Jacket", Category.JACKET, SizingSystem.MEN,
                "https://levi.in/products/mens-solid-black-jacket-002a80000", FitPreference.REGULAR,
                List.of(
                    row("S",45.72,107.95,69.22), row("M",46.99,113.03,71.12),
                    row("L",48.59,119.38,73.03), row("XL",50.17,127.00,74.93),
                    row("XXL",51.76,134.62,76.84)));

        // Women's solid regular-fit jacket.
        product(levis, "Women's Solid Regular Fit Jacket", Category.JACKET, SizingSystem.WOMEN,
                "https://levi.in/products/womens-solid-regular-fit-jacket-006h40000", FitPreference.REGULAR,
                List.of(
                    row("XXS",50.80,86.36,47.63), row("XS",52.07,91.44,48.58),
                    row("S",53.34,96.52,49.53), row("M",54.61,101.60,50.48),
                    row("L",56.20,107.95,51.44), row("XL",58.10,115.57,51.44),
                    row("XXL",60.01,123.19,51.44)));

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

        // Official Onitsuka Tiger India BODY SIZE guide. Only tops are enabled:
        // the guide is general clothing sizing, not separate numeric charts per category.
        Brand onitsuka = saveBrand("onitsuka-tiger", "Onitsuka Tiger",
                "https://www.onitsukatiger.com/in/en-in/clothing-size-guide",
                List.of(Category.T_SHIRT),
                "Official India clothing size guide. Separate men's/unisex and women's body-size rows in cm. Coverage is limited to tops; values remain UNVERIFIED pending administrator review.");
        onitsuka.setSizingSystemsByCategory(Map.of(
                Category.T_SHIRT, List.of(SizingSystem.MEN, SizingSystem.WOMEN)));
        brands.save(onitsuka);
        productInRegion(onitsuka, "Men's / Unisex Clothing Size Guide", SizingSystem.MEN,
                "https://www.onitsukatiger.com/in/en-in/clothing-size-guide",
                List.of(
                    bodyEntry("XS",78,66,78,41.4), bodyEntry("S",84,72,84,42.6),
                    bodyEntry("M",90,78,90,43.8), bodyEntry("L",96,84,96,45.0),
                    bodyEntry("XL",102,90,102,46.2)));
        productInRegion(onitsuka, "Women's Clothing Size Guide", SizingSystem.WOMEN,
                "https://www.onitsukatiger.com/in/en-in/clothing-size-guide",
                List.of(
                    bodyEntry("XS",75,59,83,37), bodyEntry("S",79,63,87,38),
                    bodyEntry("M",83,67,91,39), bodyEntry("L",87,71,95,40),
                    bodyEntry("XL",91,75,99,41)));
    }

    private void productInRegion(Brand brand, String name, SizingSystem sizingSystem,
                                 String sourceUrl, List<SizeEntry> sizes) {
        String productUrl = sourceUrl + "#" + sizingSystem.name();
        Product product = products.findByProductUrl(productUrl).orElseGet(Product::new);
        product.setName(name);
        product.setBrandId(brand.getId());
        product.setCategory(Category.T_SHIRT);
        product.setSizingSystem(sizingSystem);
        product.setRegion("IN");
        product.setProductUrl(productUrl);
        product.setFitType(FitPreference.REGULAR);
        product.setAvailableSizes(sizes.stream().map(SizeEntry::getLabel).toList());
        product.setDataSource("Official Onitsuka Tiger India clothing size guide");
        product.setVerificationStatus(VerificationStatus.UNVERIFIED);
        product.setActive(true);
        product = products.save(product);

        ProductSizeSpec spec = specs.findByProductId(product.getId()).orElseGet(ProductSizeSpec::new);
        spec.setProductId(product.getId());
        spec.setMeasurementBasis(MeasurementBasis.BODY);
        spec.setUnit(Unit.CM);
        spec.setGarmentFlat(false);
        spec.setSizes(sizes);
        spec.setDataSource("Official Onitsuka Tiger India clothing size guide");
        spec.setSourceUrl(sourceUrl);
        spec.setVerificationStatus(VerificationStatus.UNVERIFIED);
        spec.setRecommendationEligible(true);
        spec.setMeasurementNotes("Official body-size table in cm. Chest, waist, hips and shoulder values are body measurements.");
        spec.setLastVerifiedAt(null);
        specs.save(spec);
    }

    private static SizeEntry bodyEntry(String label, double chest, double waist, double hips, double shoulder) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("chest", MeasurementRange.of(chest));
        m.put("waist", MeasurementRange.of(waist));
        m.put("hips", MeasurementRange.of(hips));
        m.put("shoulder", MeasurementRange.of(shoulder));
        return new SizeEntry(label, m);
    }

    private Brand saveBrand(String slug, String name, String officialUrl,
                            List<Category> categories, String notes) {
        Brand brand = brands.findBySlug(slug).orElseGet(Brand::new);
        brand.setSlug(slug);
        brand.setName(name);
        brand.setCountry("IN");
        brand.setSupportedRegions(List.of("IN"));
        brand.setCategories(new ArrayList<>(categories));
        brand.setSizingSystemsByCategory(new EnumMap<>(Category.class));
        brand.setDataSource(officialUrl);
        brand.setVerificationStatus(VerificationStatus.UNVERIFIED);
        brand.setLastVerifiedAt(null);
        brand.setNotes(notes);
        brand.setActive(true);
        return brands.save(brand);
    }

    private void rawProduct(Brand brand, String name, Category category, SizingSystem sizingSystem,
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
        product.setDataSource("Official H&M India product page");
        product.setVerificationStatus(VerificationStatus.UNVERIFIED);
        product.setActive(false);
        product = products.save(product);

        ProductSizeSpec spec = specs.findByProductId(product.getId()).orElseGet(ProductSizeSpec::new);
        spec.setProductId(product.getId());
        spec.setMeasurementBasis(MeasurementBasis.GARMENT);
        spec.setUnit(Unit.CM);
        spec.setSizes(sizes);
        spec.setDataSource("Official H&M India product page");
        spec.setSourceUrl(sourceUrl);
        spec.setVerificationStatus(VerificationStatus.UNVERIFIED);
        spec.setRecommendationEligible(false);
        spec.setMeasurementNotes("Source publishes Width and Length only; it does not define the width axis or whether it is flat width/circumference. Stored verbatim and excluded from recommendations pending measurement-method verification.");
        spec.setLastVerifiedAt(null);
        specs.save(spec);
    }

    private void product(Brand brand, String name, Category category, SizingSystem sizingSystem,
                         String sourceUrl, FitPreference fit, List<SizeEntry> sizes) {
        product(brand, name, category, sizingSystem, sourceUrl, fit, MeasurementBasis.GARMENT, sizes);
    }

    private void product(Brand brand, String name, Category category, SizingSystem sizingSystem,
                         String sourceUrl, FitPreference fit, MeasurementBasis basis, List<SizeEntry> sizes) {
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
        spec.setMeasurementBasis(basis);
        spec.setUnit(Unit.CM);
        spec.setGarmentFlat(false);
        spec.setSizes(sizes);
        spec.setDataSource(SOURCE);
        spec.setSourceUrl(sourceUrl);
        spec.setVerificationStatus(VerificationStatus.UNVERIFIED);
        spec.setRecommendationEligible(true);
        spec.setMeasurementNotes(null);
        spec.setLastVerifiedAt(null);
        specs.save(spec);
    }

    /** UNIQLO's body-size chart labels the upper-body circumference "Bust" for women's styles;
     * normalize that equivalent body circumference to the profile's chest field. */
    private static SizeEntry widthRow(String label, double width, double length) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("width", MeasurementRange.of(width));
        m.put("length", MeasurementRange.of(length));
        return new SizeEntry(label, m);
    }

    private static SizeEntry bodyRow(String label, Number chestMin, Number chestMax,
                                     Number waistMin, Number waistMax,
                                     Number hipMin, Number hipMax) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        if (chestMin != null && chestMax != null) m.put("chest", MeasurementRange.of(chestMin.doubleValue(), chestMax.doubleValue()));
        if (waistMin != null && waistMax != null) m.put("waist", MeasurementRange.of(waistMin.doubleValue(), waistMax.doubleValue()));
        if (hipMin != null && hipMax != null) m.put("hips", MeasurementRange.of(hipMin.doubleValue(), hipMax.doubleValue()));
        return new SizeEntry(label, m);
    }

    private static SizeEntry row(String label, double shoulder, double chest, double length) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("shoulder", MeasurementRange.of(shoulder));
        m.put("chest", MeasurementRange.of(chest));
        m.put("length", MeasurementRange.of(length));
        return new SizeEntry(label, m);
    }

    private static SizeEntry jeansWithHips(String label, double waist, double hips, double thigh) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("waist", MeasurementRange.of(waist));
        m.put("hips", MeasurementRange.of(hips));
        m.put("thigh", MeasurementRange.of(thigh));
        return new SizeEntry(label, m);
    }

    private static SizeEntry bottom(String label, double waist, double hips, double thigh, double legOpening) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("waist", MeasurementRange.of(waist));
        m.put("hips", MeasurementRange.of(hips));
        m.put("thigh", MeasurementRange.of(thigh));
        m.put("legOpening", MeasurementRange.of(legOpening));
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
