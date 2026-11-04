package com.universalfit.dto;

import com.universalfit.domain.enums.Category;
import com.universalfit.domain.enums.FitPreference;
import com.universalfit.domain.enums.MeasurementBasis;
import com.universalfit.domain.enums.SizingSystem;
import com.universalfit.domain.enums.Unit;
import com.universalfit.domain.enums.VerificationStatus;
import com.universalfit.domain.model.SizeChart;
import com.universalfit.domain.model.SizeEntry;
import java.time.Instant;
import java.util.List;

/**
 * Public view of a size chart. The internal verifiedBy admin id is intentionally not exposed.
 * Always check verificationStatus: DEMO_UNVERIFIED charts are placeholders, not real brand data.
 */
public record SizeChartResponse(
        String id,
        String brandId,
        Category category,
        SizingSystem sizingSystem,
        String region,
        String collection,
        MeasurementBasis measurementBasis,
        Unit unit,
        boolean garmentFlat,
        FitPreference fitType,
        List<SizeEntry> sizes,
        String dataSource,
        String sourceUrl,
        VerificationStatus verificationStatus,
        Instant lastVerifiedAt,
        Instant updatedAt
) {
    public static SizeChartResponse from(SizeChart c) {
        return new SizeChartResponse(
                Ids.hex(c.getId()), Ids.hex(c.getBrandId()), c.getCategory(), c.getSizingSystem(),
                c.getRegion(), c.getCollection(), c.getMeasurementBasis(), c.getUnit(),
                c.isGarmentFlat(), c.getFitType(), c.getSizes(), c.getDataSource(), c.getSourceUrl(),
                c.getVerificationStatus(), c.getLastVerifiedAt(), c.getUpdatedAt());
    }
}
