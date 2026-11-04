
package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("measurement_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementProfile {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private ObjectId userId;

    @Builder.Default
    private Unit unit = Unit.CM;

    @Builder.Default
    private FitPreference preferredFit = FitPreference.REGULAR;

    /**
     * Body dimensions in the selected unit (CM or IN).
     * Weight is stored separately in kilograms.
     */
    @Builder.Default
    private Map<String, Double> values = new LinkedHashMap<>();

    private Double weightKg;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
