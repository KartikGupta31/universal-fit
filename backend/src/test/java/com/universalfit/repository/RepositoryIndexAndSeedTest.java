package com.universalfit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import com.universalfit.seed.DemoDataSeeder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@Import({MongoTestSupport.class, DemoDataSeeder.class})
class RepositoryIndexAndSeedTest {

    @Autowired MongoTemplate mongo;
    @Autowired UserRepository users;
    @Autowired BrandRepository brands;
    @Autowired SizeChartRepository charts;
    @Autowired ProductRepository products;
    @Autowired ProductSizeSpecRepository specs;
    @Autowired SavedProductRepository saved;
    @Autowired MeasurementProfileRepository profiles;
    @Autowired DemoDataSeeder seeder;

    @BeforeEach
    void clean() {
        mongo.getCollectionNames().forEach(c -> mongo.getCollection(c).deleteMany(new org.bson.Document()));
    }

    private Set<String> indexNames(String collection) {
        return mongo.getCollection(collection).listIndexes()
                .map(d -> d.getString("name")).into(new java.util.ArrayList<>())
                .stream().collect(Collectors.toSet());
    }

    @Test
    void indexesAreCreatedForFrequentlyQueriedFields() {
        // Touch each collection so auto-index-creation has run for its entity.
        users.count(); profiles.count(); charts.count(); products.count(); saved.count();
        assertThat(indexNames("users")).contains("email");
        assertThat(indexNames("measurement_profiles")).contains("userId");
        assertThat(indexNames("size_charts")).contains("chart_identity", "chart_lookup");
        assertThat(indexNames("products")).contains("product_browse");
        assertThat(indexNames("saved_products")).contains("user_product");
        assertThat(indexNames("fit_recommendations")).contains("user_created");
        assertThat(indexNames("fit_feedback")).contains("user_created", "brand_product");
        assertThat(indexNames("measurement_history")).contains("user_recorded");
    }

    @Test
    void emailMustBeUnique() {
        users.save(User.builder().email("a@example.com").passwordHash("x").build());
        assertThatThrownBy(() -> users.save(User.builder().email("a@example.com").passwordHash("y").build()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void oneMeasurementProfilePerUser() {
        ObjectId uid = new ObjectId();
        profiles.save(MeasurementProfile.builder().userId(uid).build());
        assertThatThrownBy(() -> profiles.save(MeasurementProfile.builder().userId(uid).build()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void sameChartIdentityCannotBeDuplicatedButDifferentRegionCan() {
        ObjectId brandId = new ObjectId();
        charts.save(chart(brandId, "IN", MeasurementBasis.BODY));
        charts.save(chart(brandId, "EU", MeasurementBasis.BODY));            // other region: ok
        charts.save(chart(brandId, "IN", MeasurementBasis.GARMENT));         // other basis: ok
        assertThatThrownBy(() -> charts.save(chart(brandId, "IN", MeasurementBasis.BODY)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void savedProductIsUniquePerUser() {
        ObjectId uid = new ObjectId(), pid = new ObjectId();
        saved.save(SavedProduct.builder().userId(uid).productId(pid).build());
        assertThatThrownBy(() -> saved.save(SavedProduct.builder().userId(uid).productId(pid).build()))
                .isInstanceOf(DuplicateKeyException.class);
        saved.save(SavedProduct.builder().userId(new ObjectId()).productId(pid).build()); // other user: ok
    }

    @Test
    void auditTimestampsAreSet() {
        User u = users.save(User.builder().email("t@example.com").passwordHash("x").build());
        assertThat(u.getCreatedAt()).isNotNull();
        assertThat(u.getUpdatedAt()).isNotNull();
        assertThat(u.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void paginationAndFilteringWork() {
        Brand b = brands.save(Brand.builder().slug("b1").name("B1")
                .categories(List.of(Category.JEANS)).build());
        for (int i = 0; i < 5; i++) {
            products.save(Product.builder().name("P" + i).brandId(b.getId()).category(Category.JEANS).build());
        }
        products.save(Product.builder().name("Inactive").brandId(b.getId()).category(Category.JEANS).active(false).build());
        var page = products.findByActiveTrueAndBrandIdAndCategory(b.getId(), Category.JEANS, PageRequest.of(0, 2));
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(2);
        assertThat(brands.findByActiveTrueAndCategoriesContaining(Category.JEANS, PageRequest.of(0, 10)))
                .hasSize(1);
    }

    @Test
    void seedDataIsClearlyDemoUnverifiedAndIdempotent() {
        assertThat(seeder.seed()).isTrue();
        assertThat(seeder.seed()).isFalse();   // second run creates nothing

        assertThat(brands.count()).isEqualTo(2);
        assertThat(brands.findAll()).allSatisfy(b -> {
            assertThat(b.getVerificationStatus()).isEqualTo(VerificationStatus.DEMO_UNVERIFIED);
            assertThat(b.getName()).startsWith("DEMO");
            assertThat(b.getDataSource()).contains("DEMO / UNVERIFIED");
        });
        assertThat(charts.findAll()).isNotEmpty().allSatisfy(c -> {
            assertThat(c.getVerificationStatus()).isEqualTo(VerificationStatus.DEMO_UNVERIFIED);
            assertThat(c.getLastVerifiedAt()).isNull();
            assertThat(c.getVerifiedBy()).isNull();
        });
        assertThat(charts.countByVerificationStatus(VerificationStatus.VERIFIED)).isZero();
        assertThat(products.findAll()).allSatisfy(p -> assertThat(p.getName()).contains("(DEMO)"));
        assertThat(specs.count()).isEqualTo(products.count());
    }

    private SizeChart chart(ObjectId brandId, String region, MeasurementBasis basis) {
        return SizeChart.builder().brandId(brandId).category(Category.T_SHIRT)
                .sizingSystem(SizingSystem.UNISEX).region(region).measurementBasis(basis).build();
    }
}
