package com.universalfit.seed;

import com.universalfit.domain.model.Brand;
import com.universalfit.domain.model.Product;
import com.universalfit.repository.BrandRepository;
import com.universalfit.repository.ProductRepository;
import com.universalfit.repository.ProductSizeSpecRepository;
import com.universalfit.repository.SizeChartRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * One-time, opt-in cleanup for the two fictional development brands.
 * User accounts, measurements, recommendations and feedback are intentionally untouched.
 * Enable with CLEANUP_DEMO_DATA=true for one startup, then turn it off again.
 */
@Component
@ConditionalOnProperty(name = "universalfit.cleanup.demo-data", havingValue = "true")
public class DemoDataCleanupRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataCleanupRunner.class);
    private static final Set<String> DEMO_SLUGS = Set.of("demo-alpha-wear", "demo-beta-denim");

    private final BrandRepository brands;
    private final ProductRepository products;
    private final ProductSizeSpecRepository specs;
    private final SizeChartRepository charts;

    public DemoDataCleanupRunner(BrandRepository brands, ProductRepository products,
                                 ProductSizeSpecRepository specs, SizeChartRepository charts) {
        this.brands = brands;
        this.products = products;
        this.specs = specs;
        this.charts = charts;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Brand> demoBrands = brands.findAll().stream()
                .filter(brand -> DEMO_SLUGS.contains(brand.getSlug()))
                .toList();
        int removedProducts = 0;
        int removedCharts = 0;
        for (Brand brand : demoBrands) {
            ObjectId brandId = brand.getId();
            List<Product> linkedProducts = products.findByBrandId(brandId);
            for (Product product : linkedProducts) {
                specs.deleteByProductId(product.getId());
            }
            products.deleteAll(linkedProducts);
            removedProducts += linkedProducts.size();

            var linkedCharts = charts.findByBrandId(brandId);
            removedCharts += linkedCharts.size();
            charts.deleteAll(linkedCharts);
            brands.delete(brand);
        }
        log.warn("Demo catalog cleanup complete: {} brands, {} products, {} charts removed. User data and activity history were preserved.",
                demoBrands.size(), removedProducts, removedCharts);
    }
}
