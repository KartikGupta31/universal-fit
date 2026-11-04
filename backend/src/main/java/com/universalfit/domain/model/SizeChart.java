package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A size chart for ONE brand + category + sizing system + region (+ optional collection) + basis.
 * A brand never has "one universal chart"; body and garment measurements are separate charts.
 */
@Document("size_charts")
@CompoundIndexes({
    @CompoundIndex(name = "chart_identity",
        def = "{'brandId': 1, 'category': 1, 'sizingSystem': 1, 'region': 1, 'collection': 1, 'measurementBasis': 1}",
        unique = true),
    @CompoundIndex(name = "chart_lookup", def = "{'brandId': 1, 'category': 1, 'active': 1}")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SizeChart {
    @Id private ObjectId id;
    private ObjectId brandId;
    private Category category;
    private SizingSystem sizingSystem;
    private String region;                 // e.g. "IN", "EU", "US"
    private String collection;             // optional: product line this chart applies to
    private MeasurementBasis measurementBasis;
    @Builder.Default private Unit unit = Unit.CM;
    private boolean garmentFlat;           // garment circumferences are flat-laid halves
    @Builder.Default private FitPreference fitType = FitPreference.REGULAR;
    @Builder.Default private List<SizeEntry> sizes = new ArrayList<>();
    private String dataSource;
    private String sourceUrl;
    @Indexed @Builder.Default private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    private Instant lastVerifiedAt;
    private ObjectId verifiedBy;
    @Builder.Default private boolean active = true;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
