package com.universalfit.dto;

import com.universalfit.domain.enums.Category;
import com.universalfit.domain.enums.VerificationStatus;
import com.universalfit.domain.model.Brand;
import java.time.Instant;
import java.util.List;

/** Public view of a brand. Demo brands carry verificationStatus DEMO_UNVERIFIED. */
public record BrandResponse(
        String id,
        String slug,
        String name,
        String logoUrl,
        String country,
        List<String> supportedRegions,
        List<Category> categories,
        String dataSource,
        VerificationStatus verificationStatus,
        Instant lastVerifiedAt,
        String notes
) {
    public static BrandResponse from(Brand b) {
        return new BrandResponse(
                Ids.hex(b.getId()), b.getSlug(), b.getName(), b.getLogoUrl(), b.getCountry(),
                b.getSupportedRegions(), b.getCategories(), b.getDataSource(),
                b.getVerificationStatus(), b.getLastVerifiedAt(), b.getNotes());
    }
}
