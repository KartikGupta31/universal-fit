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
 * Imports source-backed size data from official brand pages across their published regions.
 * Product-specific GARMENT charts are kept separate from BODY measurement charts.
 * Every imported source record is UNVERIFIED until an administrator reviews it.
 *
 * Enable with SEED_REAL_BRAND_DATA=true. Safe to run repeatedly; products are keyed by source URL.
 */
@Component
@ConditionalOnProperty(name = "universalfit.seed.real-brand-data", havingValue = "true")
public class RealBrandSizeDataSeeder implements ApplicationRunner {
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
                "https://th.hm.com/pages/size-guide-women", List.of(Category.T_SHIRT, Category.TROUSERS),
                "Official H&M Thailand body-measurement guides supplement India product-specific raw width/length records. TH charts are separate from IN product records; all imported records remain UNVERIFIED.");
        hm.setSupportedRegions(List.of("IN", "TH"));
        hm.setSizingSystemsByCategory(Map.of(
                Category.T_SHIRT, List.of(SizingSystem.MEN, SizingSystem.WOMEN),
                Category.TROUSERS, List.of(SizingSystem.WOMEN)));
        brands.save(hm);
        String hmWomenUrl = "https://th.hm.com/pages/size-guide-women";
        product(hm, "Women's Tops (H&M Thailand numeric EU body size guide)", Category.T_SHIRT, SizingSystem.WOMEN,
                hmWomenUrl, FitPreference.REGULAR, MeasurementBasis.BODY, "TH",
                List.of(
                    bodyRow("32",74,78,58,62,79,83), bodyRow("34",78,82,62,66,83,87),
                    bodyRow("36",82,86,66,70,87,91), bodyRow("38",86,90,70,74,91,94.5),
                    bodyRow("40",90,94,74,78,94.5,97.5), bodyRow("42",94,98,78,82,97.5,100.5),
                    bodyRow("44",98,102,82.5,87.5,100.5,103.5), bodyRow("46",102,107,87.5,93,103.5,107.5),
                    bodyRow("48",107,113,93,99,107.5,112.5), bodyRow("50",113,119,99,105,112.5,117.5),
                    bodyRow("52",119,125,105,111,117.5,122.5), bodyRow("54",125,131,111,117.5,122.5,128),
                    bodyRow("56",131,137,117.5,124.5,128,134), bodyRow("58",137,143,124.5,131.5,134,140)));
        String hmMenUrl = "https://th.hm.com/pages/size-guide-men-tops-jackets-shirts-and-blazers";
        product(hm, "Men's Tops (H&M Thailand numeric EU body size guide)", Category.T_SHIRT, SizingSystem.MEN,
                hmMenUrl, FitPreference.REGULAR, MeasurementBasis.BODY, "TH",
                List.of(
                    bodyRow("38",74,78,62,66,null,null), bodyRow("40",78,82,66,70,null,null),
                    bodyRow("42",82,86,70,74,null,null), bodyRow("44",86,90,74,78,null,null),
                    bodyRow("46",90,94,78,82,null,null), bodyRow("48",94,98,82,86,null,null),
                    bodyRow("50",98,102,86,90,null,null), bodyRow("52",102,106,90,94,null,null),
                    bodyRow("54",106,110,94,98.5,null,null), bodyRow("56",110,114,98.5,103,null,null),
                    bodyRow("58",114,118,103,107.5,null,null), bodyRow("60",118,122,107.5,112,null,null),
                    bodyRow("62",122,126,112,116.5,null,null)));
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

        seedNike();
        seedAdidas();
        seedPuma();
        seedZara();

        // Directory-only entries: do not invent size charts or recommendation categories.
        seedAdditionalBrandCharts();
    }

    /**
     * Source-backed charts for the additional directory brands. Product-page charts stay
     * product-specific; BODY charts are used only where the source explicitly labels body sizes.
     * Every imported record remains UNVERIFIED until an administrator reviews it.
     */
    private void seedAdditionalBrandCharts() {
        Brand allen = saveBrand("allen-solly", "Allen Solly",
                "https://www.nykaafashion.com/size-guide/sku?productSku=ALLAA00265799&urlRedirect=true",
                List.of(Category.SHIRT), "Men's slim-fit shirt chart from Nykaa Fashion product measurements (secondary retailer source). Values are published in inches and normalized to cm. Product-specific; UNVERIFIED.");
        allen.setCountry("IN");
        allen.setSizingSystemsByCategory(Map.of(Category.SHIRT, List.of(SizingSystem.MEN)));
        brands.save(allen);
        product(allen, "Men's Blue Slim Fit Print Full Sleeves Casual Shirt (product-specific)",
                Category.SHIRT, SizingSystem.MEN,
                "https://www.nykaafashion.com/size-guide/sku?productSku=ALLAA00265799&urlRedirect=true",
                FitPreference.SLIM, MeasurementBasis.GARMENT,
                List.of(
                    garmentInchesRow("38",40.2,16.5,null,null), garmentInchesRow("39",41.7,17.5,null,null),
                    garmentInchesRow("40",43.5,18.5,null,null), garmentInchesRow("42",46,19.5,null,null),
                    garmentInchesRow("44",48.5,20.5,null,null), garmentInchesRow("46",51,21.5,null,null)));

        Brand van = saveBrand("van-heusen", "Van Heusen",
                "https://www.nykaafashion.com/size-guide/sku?productSku=VANAA00174529&urlRedirect=true",
                List.of(Category.T_SHIRT), "Men's T-shirt chart from Nykaa Fashion product measurements (secondary retailer source). Values are published in inches and normalized to cm. Product-specific; UNVERIFIED.");
        van.setCountry("US");
        van.setSizingSystemsByCategory(Map.of(Category.T_SHIRT, List.of(SizingSystem.MEN)));
        brands.save(van);
        product(van, "Men's Blue Print Crew Neck T-shirt (product-specific)",
                Category.T_SHIRT, SizingSystem.MEN,
                "https://www.nykaafashion.com/size-guide/sku?productSku=VANAA00174529&urlRedirect=true",
                FitPreference.REGULAR, MeasurementBasis.GARMENT,
                List.of(
                    garmentInchesRow("XS",34.4,15,26,null), garmentInchesRow("S",36.5,15.5,26.5,null),
                    garmentInchesRow("M",38.5,16.5,27,null), garmentInchesRow("L",40.5,17,27.5,null),
                    garmentInchesRow("XL",42.5,17.5,28.5,null), garmentInchesRow("2XL",45,18.5,29.5,null),
                    garmentInchesRow("3XL",47.5,19.5,30.5,null)));

        Brand peter = saveBrand("peter-england", "Peter England",
                "https://www.nykaafashion.com/size-guide/sku?productSku=PETAA00228843&urlRedirect=true",
                List.of(Category.T_SHIRT), "Men's crew-neck T-shirt chart from Nykaa Fashion product measurements (secondary retailer source). Values are published in inches and normalized to cm. Product-specific; UNVERIFIED.");
        peter.setCountry("IN");
        peter.setSizingSystemsByCategory(Map.of(Category.T_SHIRT, List.of(SizingSystem.MEN)));
        brands.save(peter);
        product(peter, "Men's Black Crew Neck T-shirt (product-specific)",
                Category.T_SHIRT, SizingSystem.MEN,
                "https://www.nykaafashion.com/size-guide/sku?productSku=PETAA00228843&urlRedirect=true",
                FitPreference.REGULAR, MeasurementBasis.GARMENT,
                List.of(
                    garmentInchesRow("S",38.5,16.5,null,null), garmentInchesRow("M",40.5,17,null,null),
                    garmentInchesRow("L",42.5,17.5,null,null), garmentInchesRow("XL",45.5,18.3,null,null),
                    garmentInchesRow("2XL",48.5,19,null,null), garmentInchesRow("3XL",51.5,19.5,null,null)));

        String uspaUrl = "https://uspoloassn.in/products/menswear-solid-relaxed-fit-t-shirt-y425pwwbizz";
        Brand uspa = saveBrand("us-polo-assn", "U.S. Polo Assn.", uspaUrl,
                List.of(Category.T_SHIRT), "Official U.S. Polo Assn. India product chart. Garment chest, shoulder and length in cm; tied to the listed regular-fit T-shirt. UNVERIFIED.");
        uspa.setCountry("US");
        uspa.setSizingSystemsByCategory(Map.of(Category.T_SHIRT, List.of(SizingSystem.MEN)));
        brands.save(uspa);
        product(uspa, "Men's Solid Regular Fit T-shirt (official India product chart)",
                Category.T_SHIRT, SizingSystem.MEN, uspaUrl,
                FitPreference.REGULAR, MeasurementBasis.GARMENT,
                List.of(
                    garmentCmRow("XS",96.5,40.6,71.1,null), garmentCmRow("S",100,43.2,73.7,null),
                    garmentCmRow("M",105,45.7,74.9,null), garmentCmRow("L",114,48.3,78.7,null),
                    garmentCmRow("XL",121,50.8,80,null), garmentCmRow("XXL",127,53.3,81.3,null),
                    garmentCmRow("XXXL",133,55.9,82.5,null), garmentCmRow("XXXXL",138,58.4,83.8,null),
                    garmentCmRow("XXXXXL",141,61,88.9,null)));

        String jackUrl = "https://www.jackjones.in/products/902274101-logo-print-slim-fit-o-neck-t-shirt-white";
        Brand jack = saveBrand("jack-jones", "Jack & Jones", jackUrl,
                List.of(Category.T_SHIRT), "Men's organic cotton slim-fit T-shirt chart shown on Jack & Jones India product listing (brand-owned source). Values are in cm. Product-specific; UNVERIFIED.");
        jack.setCountry("DK");
        jack.setSizingSystemsByCategory(Map.of(Category.T_SHIRT, List.of(SizingSystem.MEN)));
        brands.save(jack);
        product(jack, "Men's Organic Cotton Slim Fit Crew Neck T-shirt (product-specific)",
                Category.T_SHIRT, SizingSystem.MEN, jackUrl,
                FitPreference.SLIM, MeasurementBasis.GARMENT,
                List.of(
                    garmentCmRow("S",94,41.9,68.1,18), garmentCmRow("M",98,43.4,70.1,19.1),
                    garmentCmRow("L",102.1,45,71.9,20.1), garmentCmRow("XL",110,47.5,73.9,21.1),
                    garmentCmRow("XXL",118.1,50,75.9,22.1)));

        String tommyUrl = "https://tommy.colanapps.in/mens-size-guide";
        Brand tommy = saveBrand("tommy-hilfiger", "Tommy Hilfiger", tommyUrl,
                List.of(Category.T_SHIRT, Category.SHIRT), "Men's body size guide from Tommy Hilfiger India size-guide partner page (secondary/partner source). Chest and waist ranges in cm; verify against individual product charts. UNVERIFIED.");
        tommy.setCountry("US");
        tommy.setSizingSystemsByCategory(Map.of(
                Category.T_SHIRT, List.of(SizingSystem.MEN),
                Category.SHIRT, List.of(SizingSystem.MEN)));
        brands.save(tommy);
        product(tommy, "Men's Tops and Casual Shirts (body size guide)",
                Category.T_SHIRT, SizingSystem.MEN, tommyUrl,
                FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    bodyRow("XS",88,92,78,82,null,null), bodyRow("S",93,97,83,87,null,null),
                    bodyRow("M",98,102,88,92,null,null), bodyRow("L",103,108,93,98,null,null),
                    bodyRow("XL",109,114,99,104,null,null), bodyRow("XXL",115,120,105,110,null,null),
                    bodyRow("3XL",121,126,111,116,null,null)));
        product(tommy, "Men's Casual Shirts (body size guide)",
                Category.SHIRT, SizingSystem.MEN, tommyUrl + "#shirts",
                FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    bodyRow("XS",88,92,78,82,null,null), bodyRow("S",93,97,83,87,null,null),
                    bodyRow("M",98,102,88,92,null,null), bodyRow("L",103,108,93,98,null,null),
                    bodyRow("XL",109,114,99,104,null,null), bodyRow("XXL",115,120,105,110,null,null),
                    bodyRow("3XL",121,126,111,116,null,null)));

        String ckUrl = "https://www.nykaafashion.com/size-guide/sku?productSku=CALVI00011381&urlRedirect=true";
        Brand ck = saveBrand("calvin-klein", "Calvin Klein", ckUrl,
                List.of(Category.T_SHIRT), "Men's regular-fit T-shirt chart from Nykaa Fashion product measurements (secondary retailer source). Values are published in inches and normalized to cm. Product-specific; UNVERIFIED.");
        ck.setCountry("US");
        ck.setSizingSystemsByCategory(Map.of(Category.T_SHIRT, List.of(SizingSystem.MEN)));
        brands.save(ck);
        product(ck, "Men's Logo Regular-Fit Cotton T-shirt (product-specific)",
                Category.T_SHIRT, SizingSystem.MEN, ckUrl,
                FitPreference.REGULAR, MeasurementBasis.GARMENT,
                List.of(
                    garmentInchesRow("XS",37,16.1,26.6,null), garmentInchesRow("S",38,16.6,27.6,null),
                    garmentInchesRow("M",40,17.1,28.4,null), garmentInchesRow("L",42,17.6,29.1,null),
                    garmentInchesRow("XL",44,18.1,29.9,null), garmentInchesRow("2XL",46,18.7,30.7,null)));

        String bibaUrl = "https://www.nykaafashion.com/size-guide/sku?productSku=BIBAX00028419&urlRedirect=true";
        Brand biba = saveBrand("biba", "BIBA", bibaUrl,
                List.of(Category.TOP), "Women's kurta body-fit chart from Nykaa Fashion (secondary retailer source). Published body bust/waist/hip values are in inches and normalized to cm. Product-specific fit reference; UNVERIFIED.");
        biba.setCountry("IN");
        biba.setSizingSystemsByCategory(Map.of(
                Category.TOP, List.of(SizingSystem.WOMEN)));
        brands.save(biba);
        product(biba, "Women's Straight Kurta (body measurements this product fits)",
                Category.TOP, SizingSystem.WOMEN, bibaUrl,
                FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    bodyInchesRow("32",32,33,28,29,36,37), bodyInchesRow("34",34,35,30,31,38,39),
                    bodyInchesRow("36",36,37,32,33,40,41), bodyInchesRow("38",38,39,34,35,42,43),
                    bodyInchesRow("40",40,41,36,38,44,45), bodyInchesRow("42",42,43,40,41,47,48)));

        String fabUrl = "https://www.nykaafashion.com/size-guide/sku?productSku=FABIN00028001&urlRedirect=true";
        Brand fab = saveBrand("fabindia", "Fabindia", fabUrl,
                List.of(Category.TOP), "Women's kurta body-fit chart from Nykaa Fashion (secondary retailer source). Body bust/waist/hip values are in inches and normalized to cm. Product-specific fit reference; UNVERIFIED.");
        fab.setCountry("IN");
        fab.setSizingSystemsByCategory(Map.of(Category.TOP, List.of(SizingSystem.WOMEN)));
        brands.save(fab);
        product(fab, "Women's Cotton Printed Kurta (body measurements this product fits)",
                Category.TOP, SizingSystem.WOMEN, fabUrl,
                FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    bodyInchesRow("XS",32,32,26,26,35,35), bodyInchesRow("S",34,34,28,28,37,37),
                    bodyInchesRow("M",36,36,30,30,39,39), bodyInchesRow("L",39,39,33,33,42,42),
                    bodyInchesRow("XL",42,42,36,36,45,45)));

        String wUrl = "https://sizly.app/brands/w";
        Brand w = saveBrand("w-for-woman", "W for Woman", wUrl,
                List.of(Category.TOP), "Women's body size chart reproduced by Sizly from a W for Woman Amazon India listing (secondary source, checked Oct 2026). Values in inches normalized to cm; UNVERIFIED.");
        w.setCountry("IN");
        w.setSizingSystemsByCategory(Map.of(Category.TOP, List.of(SizingSystem.WOMEN)));
        brands.save(w);
        product(w, "Women's Apparel (secondary body-size reference)",
                Category.TOP, SizingSystem.WOMEN, wUrl,
                FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    bodyInchesRow("XS",31,31,28,28,36,36), bodyInchesRow("S",34,34,31,31,39,39),
                    bodyInchesRow("M",36,36,33,33,41,41), bodyInchesRow("L",38,38,35,35,42,42),
                    bodyInchesRow("XL",39,39,37,37,44,44), bodyInchesRow("2XL",41,41,39,39,46,46),
                    bodyInchesRow("3XL",46,46,44,44,51,51), bodyInchesRow("4XL",48.5,48.5,47,47,53,53),
                    bodyInchesRow("5XL",51,51,49.5,49.5,55.5,55.5)));

        String decathlonUrl = "https://d1314cmsbd81ch.cloudfront.net/size_guide/sizeguide_en.html";
        Brand decathlon = saveBrand("decathlon", "Decathlon", decathlonUrl,
                List.of(Category.T_SHIRT, Category.TROUSERS), "Official Decathlon clothing size guide. Body measurements in cm. Men's and women's tops and bottoms are separate charts; imported rows remain UNVERIFIED.");
        decathlon.setCountry("FR");
        decathlon.setSizingSystemsByCategory(Map.of(
                Category.T_SHIRT, List.of(SizingSystem.MEN, SizingSystem.WOMEN),
                Category.TROUSERS, List.of(SizingSystem.MEN, SizingSystem.WOMEN)));
        brands.save(decathlon);
        product(decathlon, "Men's Tops (official body size guide)",
                Category.T_SHIRT, SizingSystem.MEN, decathlonUrl + "#mens-tops",
                FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    bodyRow("XS",84,87,null,null,null,null), bodyRow("XS/S",88,91,null,null,null,null),
                    bodyRow("S",92,95,null,null,null,null), bodyRow("S/M",96,99,null,null,null,null),
                    bodyRow("M",100,103,null,null,null,null), bodyRow("M/L",104,107,null,null,null,null),
                    bodyRow("L",108,113,null,null,null,null), bodyRow("L/XL",114,118,null,null,null,null),
                    bodyRow("XL",119,123,null,null,null,null), bodyRow("XL/2XL",124,129,null,null,null,null),
                    bodyRow("2XL",130,135,null,null,null,null), bodyRow("2XL/3XL",136,141,null,null,null,null),
                    bodyRow("3XL",142,146,null,null,null,null), bodyRow("3XL/4XL",147,153,null,null,null,null),
                    bodyRow("4XL",154,159,null,null,null,null)));
        product(decathlon, "Men's Bottoms (official body size guide)", Category.TROUSERS, SizingSystem.MEN,
                decathlonUrl + "#mens-bottoms", FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    waistBodyRow("36",70,73), waistBodyRow("38",74,77), waistBodyRow("40",78,81),
                    waistBodyRow("42",82,85), waistBodyRow("44",86,89), waistBodyRow("46",90,95),
                    waistBodyRow("48",96,100), waistBodyRow("50",101,105), waistBodyRow("52",106,111),
                    waistBodyRow("54",112,117), waistBodyRow("56",118,123), waistBodyRow("58",124,129),
                    waistBodyRow("60",130,135), waistBodyRow("62",136,141), waistBodyRow("64",142,147)));

        String pepeUrl = "https://www.nykaafashion.com/size-guide/sku?productSku=PEPEJ00069108&urlRedirect=true";
        Brand pepe = saveBrand("pepe-jeans", "Pepe Jeans", pepeUrl,
                List.of(Category.SHIRT), "Men's checked shirt product measurements from Nykaa Fashion (secondary retailer source). Values are published in inches and normalized to cm. Product-specific; UNVERIFIED.");
        pepe.setCountry("GB");
        pepe.setSizingSystemsByCategory(Map.of(Category.SHIRT, List.of(SizingSystem.MEN)));
        brands.save(pepe);
        product(pepe, "Men's Multi Color Checked Full Sleeved Shirt (product-specific)",
                Category.SHIRT, SizingSystem.MEN, pepeUrl,
                FitPreference.REGULAR, MeasurementBasis.GARMENT,
                List.of(
                    garmentInchesRow("S",43.3,18.5,28.1,24.8), garmentInchesRow("M",44.9,19.3,28.9,25.2),
                    garmentInchesRow("L",46.5,20.1,29.9,25.6), garmentInchesRow("XL",48,20.9,30.9,26),
                    garmentInchesRow("2XL",50.4,21.7,31.7,26.4), garmentInchesRow("3XL",53.5,22.4,31.7,26.4)));

        product(decathlon, "Women's Tops (official body size guide)",
                Category.T_SHIRT, SizingSystem.WOMEN, decathlonUrl + "#womens-tops",
                FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    bodyRow("2",76,79,null,null,null,null), bodyRow("4",80,83,null,null,null,null),
                    bodyRow("6",84,87,null,null,null,null), bodyRow("8",86,91,null,null,null,null),
                    bodyRow("10",92,95,null,null,null,null), bodyRow("12",96,99,null,null,null,null),
                    bodyRow("14",100,103,null,null,null,null), bodyRow("16",104,107,null,null,null,null),
                    bodyRow("18",108,113,null,null,null,null), bodyRow("20",114,118,null,null,null,null),
                    bodyRow("22",119,123,null,null,null,null), bodyRow("24",124,129,null,null,null,null),
                    bodyRow("26",130,135,null,null,null,null), bodyRow("28",136,141,null,null,null,null),
                    bodyRow("30",142,146,null,null,null,null), bodyRow("32",147,153,null,null,null,null)));
        product(decathlon, "Women's Bottoms (official body size guide)",
                Category.TROUSERS, SizingSystem.WOMEN, decathlonUrl + "#womens-bottoms",
                FitPreference.REGULAR, MeasurementBasis.BODY,
                List.of(
                    waistBodyRow("32",57,58), waistBodyRow("34",58,62), waistBodyRow("36",63,65),
                    waistBodyRow("38",66,69), waistBodyRow("40",70,73), waistBodyRow("42",74,77),
                    waistBodyRow("44",78,84), waistBodyRow("46",85,90), waistBodyRow("48",91,96),
                    waistBodyRow("50",97,102), waistBodyRow("52",103,108), waistBodyRow("54",109,114),
                    waistBodyRow("56",115,120), waistBodyRow("58",121,128), waistBodyRow("60",129,132),
                    waistBodyRow("62",133,139)));

        // Bonkers Corner's retailer table labels the field "Product Chest" but the extracted
        // source does not establish whether it is a circumference or a flat width. Keep it
        // visible to administrators but explicitly ineligible for recommendations until checked.
        String bonkersUrl = "https://www.nykaafashion.com/size-guide/sku?productSku=BONKE00000341&urlRedirect=true";
        Brand bonkers = saveBrand("bonkers-corner", "Bonkers Corner", bonkersUrl,
                List.of(), "A secondary retailer lists product chest values, but the measurement method is not explicit. Imported as admin-review-only and excluded from recommendations until validated.");
        bonkers.setCountry("IN");
        bonkers.setSizingSystemsByCategory(new EnumMap<>(Category.class));
        brands.save(bonkers);
        rawPendingProduct(bonkers, "Men's Black Printed T-shirt (raw retailer chest table)",
                Category.T_SHIRT, SizingSystem.MEN, bonkersUrl,
                List.of(
                    chestOnlyRow("XS",41), chestOnlyRow("S",43), chestOnlyRow("M",45),
                    chestOnlyRow("L",47), chestOnlyRow("XL",49), chestOnlyRow("2XL",51)),
                "Nykaa Fashion labels this field Product Chest and displays 41–51 in its size-guide table. The source extraction does not establish flat-vs-circumference method. Stored for administrator review only; not recommendation eligible.");
    }

    private static SizeEntry chestOnlyRow(String label, double chest) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("chest", MeasurementRange.of(chest));
        return new SizeEntry(label, m);
    }

    private static SizeEntry waistBodyRow(String label, double waistMin, double waistMax) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("waist", MeasurementRange.of(waistMin, waistMax));
        return new SizeEntry(label, m);
    }

    private static SizeEntry bodyInchesRow(String label, double chestMin, double chestMax,
                                           double waistMin, double waistMax,
                                           double hipMin, double hipMax) {
        return bodyRow(label, chestMin * 2.54, chestMax * 2.54,
                waistMin * 2.54, waistMax * 2.54, hipMin * 2.54, hipMax * 2.54);
    }

    private static SizeEntry garmentInchesRow(String label, Double chest, Double shoulder,
                                              Double length, Double sleeve) {
        return garmentCmRow(label, chest == null ? null : chest * 2.54,
                shoulder == null ? null : shoulder * 2.54,
                length == null ? null : length * 2.54,
                sleeve == null ? null : sleeve * 2.54);
    }

    private static SizeEntry garmentCmRow(String label, Double chest, Double shoulder,
                                          Double length, Double sleeve) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        if (chest != null) m.put("chest", MeasurementRange.of(chest));
        if (shoulder != null) m.put("shoulder", MeasurementRange.of(shoulder));
        if (length != null) m.put("length", MeasurementRange.of(length));
        if (sleeve != null) m.put("sleeve", MeasurementRange.of(sleeve));
        return new SizeEntry(label, m);
    }

    private void rawPendingProduct(Brand brand, String name, Category category, SizingSystem sizingSystem,
                                   String sourceUrl, List<SizeEntry> sizes, String notes) {
        Product product = products.findByProductUrl(sourceUrl).orElseGet(Product::new);
        product.setName(name);
        product.setBrandId(brand.getId());
        product.setCategory(category);
        product.setSizingSystem(sizingSystem);
        product.setRegion("IN");
        product.setProductUrl(sourceUrl);
        product.setFitType(FitPreference.REGULAR);
        product.setAvailableSizes(sizes.stream().map(SizeEntry::getLabel).toList());
        product.setDataSource("Nykaa Fashion secondary retailer size guide");
        product.setVerificationStatus(VerificationStatus.UNVERIFIED);
        product.setActive(false);
        product = products.save(product);
        ProductSizeSpec spec = specs.findByProductId(product.getId()).orElseGet(ProductSizeSpec::new);
        spec.setProductId(product.getId());
        spec.setMeasurementBasis(MeasurementBasis.GARMENT);
        spec.setUnit(Unit.CM);
        spec.setGarmentFlat(false);
        spec.setSizes(sizes);
        spec.setDataSource("Nykaa Fashion secondary retailer size guide");
        spec.setSourceUrl(sourceUrl);
        spec.setVerificationStatus(VerificationStatus.UNVERIFIED);
        spec.setRecommendationEligible(false);
        spec.setMeasurementNotes(notes);
        spec.setLastVerifiedAt(null);
        specs.save(spec);
    }

    private Brand catalogBrand(String slug, String name, String origin, String officialUrl) {
        Brand brand = saveBrand(slug, name, officialUrl, List.of(),
                "Brand directory entry only. No size chart data has been imported yet; categories will appear only after source-backed size charts are added.");
        brand.setCountry(origin);
        brand.setSupportedRegions(List.of("IN"));
        brand.setSizingSystemsByCategory(new EnumMap<>(Category.class));
        return brands.save(brand);
    }

    private static SizeEntry bodyEntry(String label, double chest, double waist, double hips, double shoulder) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("chest", MeasurementRange.of(chest));
        m.put("waist", MeasurementRange.of(waist));
        m.put("hips", MeasurementRange.of(hips));
        m.put("shoulder", MeasurementRange.of(shoulder));
        return new SizeEntry(label, m);
    }

    private void seedNike() {
        String menUrl = "https://www.nike.com/size-fit/mens-tops-asian-alpha";
        String womenUrl = "https://img.nike.com.hk/resources/sizecart/asian-womens-tops-sizing-chart_en.html";
        Brand nike = saveBrand("nike", "Nike", menUrl, List.of(Category.T_SHIRT, Category.TROUSERS),
                "Official Nike Asian body-size charts for men's and women's tops and women's bottoms. Region is ASIA, not India; records remain UNVERIFIED.");
        nike.setCountry("US");
        nike.setSupportedRegions(List.of("ASIA"));
        nike.setSizingSystemsByCategory(Map.of(
                Category.T_SHIRT, List.of(SizingSystem.MEN, SizingSystem.WOMEN),
                Category.TROUSERS, List.of(SizingSystem.WOMEN)));
        brands.save(nike);
        product(nike, "Men's Tops (Asian Sizing)", Category.T_SHIRT, SizingSystem.MEN,
                menUrl, FitPreference.REGULAR, MeasurementBasis.BODY, "ASIA",
                List.of(
                    nikeEntry("XS",80,67,73,81.5,86.5), nikeEntry("S",84,73,79,86.5,91.5),
                    nikeEntry("M",88,79,85,91.5,96.5), nikeEntry("L",92,85,91,96.5,101.5),
                    nikeEntry("XL",96,91,97,101.5,106.5), nikeEntry("2XL",100,97,104,106.5,112),
                    nikeEntry("3XL",104,104,112,112,118), nikeEntry("4XL",108,112,120,118,123)));
        product(nike, "Women's Tops (Asian Sizing)", Category.T_SHIRT, SizingSystem.WOMEN,
                womenUrl, FitPreference.REGULAR, MeasurementBasis.BODY, "ASIA",
                List.of(
                    nikeEntry("XXS",72,62.5,65.5,83.5,86.5), nikeEntry("XS",76,65.5,68.5,86.5,89.5),
                    nikeEntry("S",80,68.5,71.5,89.5,92.5), nikeEntry("M",84,71.5,75.5,92.5,96.5),
                    nikeEntry("L",88,75.5,80.5,96.5,101.5), nikeEntry("XL",92,80.5,85.5,101.5,106.5),
                    nikeEntry("2XL",96,85.5,90.5,106.5,111.5)));
        String womenBottomsUrl = "https://img.nike.com.hk/resources/sizecart/asian-womens-pants-sizing-chart_en.html";
        product(nike, "Women's Bottoms (Asian Sizing)", Category.TROUSERS, SizingSystem.WOMEN,
                womenBottomsUrl, FitPreference.REGULAR, MeasurementBasis.BODY, "ASIA",
                List.of(
                    nikeBottomRow("XXS",54,54,83.5,86.5), nikeBottomRow("XS",58,58,86.5,89.5),
                    nikeBottomRow("S",62,62,89.5,92.5), nikeBottomRow("M",66,66,92.5,96.5),
                    nikeBottomRow("L",70,70,96.5,101.5), nikeBottomRow("XL",74,74,101.5,106.5),
                    nikeBottomRow("2XL",78,78,106.5,111.5)));
    }

    private void seedAdidas() {
        String url = "https://www.adidas.com/us/help/size_charts";
        Brand adidas = saveBrand("adidas", "adidas", url, List.of(Category.T_SHIRT, Category.TROUSERS),
                "Official adidas US body-size charts for men's tops, women's tops and women's bottoms. Region is US, not India; imported rows remain UNVERIFIED.");
        adidas.setCountry("DE");
        adidas.setSupportedRegions(List.of("US"));
        adidas.setSizingSystemsByCategory(Map.of(
                Category.T_SHIRT, List.of(SizingSystem.MEN, SizingSystem.WOMEN),
                Category.TROUSERS, List.of(SizingSystem.MEN, SizingSystem.WOMEN)));
        brands.save(adidas);
        product(adidas, "Men's Shirts & Tops (US Sizing)", Category.T_SHIRT, SizingSystem.MEN,
                url, FitPreference.REGULAR, MeasurementBasis.BODY, "US",
                List.of(
                    bodyRow("XS",83,86,71,74,82,85), bodyRow("S",87,92,75,80,86,91),
                    bodyRow("M",93,100,81,88,92,99), bodyRow("L",101,108,89,96,100,107),
                    bodyRow("XL",109,118,97,106,108,116), bodyRow("2XL",119,130,107,119,117,125),
                    bodyRow("3XL",131,142,120,132,126,135)));
        String womenTopsUrl = "https://www.adidas.com/us/help/size_charts/women-shirts_tops/";
        product(adidas, "Women's Shirts & Tops (US Sizing)", Category.T_SHIRT, SizingSystem.WOMEN,
                womenTopsUrl, FitPreference.REGULAR, MeasurementBasis.BODY, "US",
                List.of(
                    bodyRow("2XS",73,76,57,60,82,85), bodyRow("XS",77,82,61,66,86,91),
                    bodyRow("S",83,88,67,72,92,97), bodyRow("M",89,94,73,78,98,103),
                    bodyRow("L",95,101,79,85,104,110), bodyRow("XL",102,109,86,94,111,117),
                    bodyRow("2XL",110,118,95,104,118,125)));
        String womenBottomsUrl = "https://www.adidas.com/us/help/size_charts/women-pants_shorts";
        product(adidas, "Women's Pants & Shorts (US Sizing)", Category.TROUSERS, SizingSystem.WOMEN,
                womenBottomsUrl, FitPreference.REGULAR, MeasurementBasis.BODY, "US",
                List.of(
                    bodyBottomRow("2XS",57,60,82,85,77.5), bodyBottomRow("XS",61,66,86,91,78),
                    bodyBottomRow("S",67,72,92,97,78.5), bodyBottomRow("M",73,78,98,103,79),
                    bodyBottomRow("L",79,85,104,110,79.5), bodyBottomRow("XL",86,94,111,117,80),
                    bodyBottomRow("2XL",95,104,118,125,80.5)));
        String menBottomsUrl = "https://www.adidas.com/us/help/size_charts/men-pants_shorts";
        product(adidas, "Men's Pants & Shorts (US Sizing)", Category.TROUSERS, SizingSystem.MEN,
                menBottomsUrl, FitPreference.REGULAR, MeasurementBasis.BODY, "US",
                List.of(
                    bodyBottomRow("XS Short",71,74,82,85,76), bodyBottomRow("S Short",75,80,86,91,76.5),
                    bodyBottomRow("M Short",81,88,92,99,77), bodyBottomRow("L Short",89,96,100,107,77.5),
                    bodyBottomRow("XL Short",97,106,108,116,78), bodyBottomRow("2XL Short",107,119,117,125,77.5),
                    bodyBottomRow("3XL Short",120,132,126,135,77)));
    }

    private void seedPuma() {
        String url = "https://weekendsports.co.uk/pages/puma-size-guide";
        Brand puma = saveBrand("puma", "PUMA", url,
                List.of(Category.T_SHIRT, Category.TROUSERS),
                "Secondary retailer guide reproduces PUMA published body-size ranges and labels the chart UK sizing. Region is UK; imported data remains UNVERIFIED pending admin source review.");
        puma.setCountry("DE");
        puma.setSupportedRegions(List.of("UK"));
        puma.setSizingSystemsByCategory(Map.of(
                Category.T_SHIRT, List.of(SizingSystem.MEN, SizingSystem.WOMEN),
                Category.TROUSERS, List.of(SizingSystem.WOMEN)));
        brands.save(puma);
        product(puma, "Men's Tops (PUMA UK body size reference)", Category.T_SHIRT, SizingSystem.MEN,
                url + "#mens-tops", FitPreference.REGULAR, MeasurementBasis.BODY, "UK",
                List.of(
                    bodyRow("XXS",73,80,60,67,null,null), bodyRow("XS",81,88,68,75,null,null),
                    bodyRow("S",89,96,76,83,null,null), bodyRow("M",97,104,84,91,null,null),
                    bodyRow("L",105,113,92,100,null,null), bodyRow("XL",114,123,101,110,null,null),
                    bodyRow("2XL",124,133,111,121,null,null), bodyRow("3XL",134,143,122,132,null,null),
                    bodyRow("4XL",144,153,133,143,null,null)));
        product(puma, "Women's Tops (PUMA UK body size reference)", Category.T_SHIRT, SizingSystem.WOMEN,
                url + "#womens-tops", FitPreference.REGULAR, MeasurementBasis.BODY, "UK",
                List.of(
                    bodyRow("XXS",75,79,59,63,83,87), bodyRow("XS",80,84,64,68,88,92),
                    bodyRow("S",85,90,69,74,93,98), bodyRow("M",91,98,75,82,99,106),
                    bodyRow("L",99,106,83,90,107,114), bodyRow("XL",107,114,91,99,115,122),
                    bodyRow("2XL",115,122,100,108,123,130), bodyRow("3XL",123,130,109,117,131,138)));
        product(puma, "Women's Bottoms (PUMA UK body size reference)", Category.TROUSERS, SizingSystem.WOMEN,
                url + "#womens-bottoms", FitPreference.REGULAR, MeasurementBasis.BODY, "UK",
                List.of(
                    pumaBottomRow("XXS",59,63,83,87), pumaBottomRow("XS",64,68,88,92),
                    pumaBottomRow("S",69,74,93,98), pumaBottomRow("M",75,82,99,106),
                    pumaBottomRow("L",83,90,107,114), pumaBottomRow("XL",91,99,115,122),
                    pumaBottomRow("2XL",100,108,123,130), pumaBottomRow("3XL",109,117,131,138)));
    }

    private void seedZara() {
        String url = "https://www.sizecharter.com/brands/zar/womens";
        Brand zara = saveBrand("zara", "Zara", url,
                List.of(Category.T_SHIRT, Category.TROUSERS, Category.DRESS, Category.JACKET),
                "Secondary source SizeCharter publishes Zara women's alpha-size body measurements in centimetres. Market/region is not specified by the source, so GLOBAL is a reference region only; individual Zara product charts take precedence. All records remain UNVERIFIED.");
        zara.setCountry("ES");
        zara.setSupportedRegions(List.of("GLOBAL"));
        zara.setSizingSystemsByCategory(Map.of(
                Category.T_SHIRT, List.of(SizingSystem.WOMEN),
                Category.TROUSERS, List.of(SizingSystem.WOMEN),
                Category.DRESS, List.of(SizingSystem.WOMEN),
                Category.JACKET, List.of(SizingSystem.WOMEN)));
        brands.save(zara);
        // SizeCharter publishes this chart in inches; convert to centimetres (x 2.54).
        List<SizeEntry> tops = List.of(
                zaraTopRow("XXS",80.01,57.79,85.73), zaraTopRow("XS",81.92,62.23,90.17),
                zaraTopRow("S",86.36,66.04,93.98), zaraTopRow("M",90.17,69.85,97.79),
                zaraTopRow("L",95.89,76.20,104.14), zaraTopRow("XL",102.24,81.92,109.86));
        product(zara, "Women's Tops (Zara secondary body-size reference)", Category.T_SHIRT, SizingSystem.WOMEN,
                url + "#womens-tops", FitPreference.REGULAR, MeasurementBasis.BODY, "GLOBAL", tops);
        product(zara, "Women's Dresses (Zara secondary body-size reference)", Category.DRESS, SizingSystem.WOMEN,
                url + "#womens-dresses", FitPreference.REGULAR, MeasurementBasis.BODY, "GLOBAL", tops);
        product(zara, "Women's Bottoms (Zara secondary body-size reference)", Category.TROUSERS, SizingSystem.WOMEN,
                url + "#womens-bottoms", FitPreference.REGULAR, MeasurementBasis.BODY, "GLOBAL",
                List.of(
                    zaraBottomRow("XXS",57.79,85.73), zaraBottomRow("XS",62.23,90.17),
                    zaraBottomRow("S",66.04,93.98), zaraBottomRow("M",69.85,97.79),
                    zaraBottomRow("L",76.20,104.14), zaraBottomRow("XL",81.92,109.86)));
        product(zara, "Women's Jackets (Zara secondary body-size reference)", Category.JACKET, SizingSystem.WOMEN,
                url + "#womens-jackets", FitPreference.REGULAR, MeasurementBasis.BODY, "GLOBAL", tops);
    }

    private static SizeEntry zaraTopRow(String label, double bust, double waist, double hips) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("chest", MeasurementRange.of(bust));
        m.put("waist", MeasurementRange.of(waist));
        m.put("hips", MeasurementRange.of(hips));
        return new SizeEntry(label, m);
    }

    private static SizeEntry zaraBottomRow(String label, double waist, double hips) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("waist", MeasurementRange.of(waist));
        m.put("hips", MeasurementRange.of(hips));
        return new SizeEntry(label, m);
    }

    private static SizeEntry nikeBottomRow(String label, double waistMin, double waistMax,
                                           double hipMin, double hipMax) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("waist", MeasurementRange.of(waistMin, waistMax));
        m.put("hips", MeasurementRange.of(hipMin, hipMax));
        return new SizeEntry(label, m);
    }

    private static SizeEntry bodyBottomRow(String label, double waistMin, double waistMax,
                                           double hipMin, double hipMax, double inseam) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("waist", MeasurementRange.of(waistMin, waistMax));
        m.put("hips", MeasurementRange.of(hipMin, hipMax));
        m.put("inseam", MeasurementRange.of(inseam));
        return new SizeEntry(label, m);
    }

    private static SizeEntry pumaBottomRow(String label, double waistMin, double waistMax,
                                            double hipMin, double hipMax) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("waist", MeasurementRange.of(waistMin, waistMax));
        m.put("hips", MeasurementRange.of(hipMin, hipMax));
        return new SizeEntry(label, m);
    }

    private static SizeEntry nikeEntry(String label, double chest,
                                       double waistMin, double waistMax,
                                       double hipMin, double hipMax) {
        Map<String, MeasurementRange> m = new LinkedHashMap<>();
        m.put("chest", MeasurementRange.of(chest));
        m.put("waist", MeasurementRange.of(waistMin, waistMax));
        m.put("hips", MeasurementRange.of(hipMin, hipMax));
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
        product(brand, name, category, sizingSystem, sourceUrl, fit, basis, "IN", sizes);
    }

    private void product(Brand brand, String name, Category category, SizingSystem sizingSystem,
                         String sourceUrl, FitPreference fit, MeasurementBasis basis,
                         String region, List<SizeEntry> sizes) {
        Product product = products.findByProductUrl(sourceUrl).orElseGet(Product::new);
        product.setName(name);
        product.setBrandId(brand.getId());
        product.setCategory(category);
        product.setSizingSystem(sizingSystem);
        product.setRegion(region);
        product.setProductUrl(sourceUrl);
        product.setFitType(fit);
        product.setAvailableSizes(sizes.stream().map(SizeEntry::getLabel).toList());
        product.setDataSource(sourceUrl);
        product.setVerificationStatus(VerificationStatus.UNVERIFIED);
        product.setActive(true);
        product = products.save(product);

        ProductSizeSpec spec = specs.findByProductId(product.getId()).orElseGet(ProductSizeSpec::new);
        spec.setProductId(product.getId());
        spec.setMeasurementBasis(basis);
        spec.setUnit(Unit.CM);
        spec.setGarmentFlat(false);
        spec.setSizes(sizes);
        spec.setDataSource(sourceUrl);
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
