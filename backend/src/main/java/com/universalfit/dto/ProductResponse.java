package com.universalfit.dto;

import com.universalfit.domain.enums.Category;
import com.universalfit.domain.enums.FitPreference;
import com.universalfit.domain.enums.SizingSystem;
import com.universalfit.domain.enums.VerificationStatus;
import com.universalfit.domain.model.Product;
import java.time.Instant;
import java.util.List;

/** Public view of a product. */
public record ProductResponse(
        String id,
        String name,
        String brandId,
        Category category,
        SizingSystem sizingSystem,
        String description,
        String imageUrl,
        String productUrl,
        List<String> availableSizes,
        FitPreference fitType,
        Double price,
        String currency,
        String region,
        String dataSource,
        VerificationStatus verificationStatus,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                Ids.hex(p.getId()), p.getName(), Ids.hex(p.getBrandId()), p.getCategory(),
                p.getSizingSystem(), p.getDescription(), p.getImageUrl(), p.getProductUrl(),
                p.getAvailableSizes(), p.getFitType(), p.getPrice(), p.getCurrency(), p.getRegion(),
                p.getDataSource(), p.getVerificationStatus(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
