package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

/** Product-specific body or garment sizing data. The explicit basis determines how measurements are interpreted. */
@Document("product_size_specs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductSizeSpec {
    @Id private ObjectId id;
    @Indexed(unique = true) private ObjectId productId;
    @Builder.Default private MeasurementBasis measurementBasis = MeasurementBasis.GARMENT;
    @Builder.Default private Unit unit = Unit.CM;
    private boolean garmentFlat;
    @Builder.Default private List<SizeEntry> sizes = new ArrayList<>();
    private String dataSource;
    /** Exact official product page containing this style's size chart. */
    private String sourceUrl;
    /** False when source dimensions are retained verbatim but are not safe for recommendations yet. */
    @Builder.Default private Boolean recommendationEligible = true;
    private String measurementNotes;
    @Builder.Default private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    private Instant lastVerifiedAt;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
