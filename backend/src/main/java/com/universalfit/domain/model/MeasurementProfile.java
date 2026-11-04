package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

/** A user's current body measurements. Private by default: only reachable via the owner's own endpoints. */
@Document("measurement_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeasurementProfile {
    @Id private ObjectId id;
    @Indexed(unique = true) private ObjectId userId;    // one profile per user
    @Builder.Default private Unit unit = Unit.CM;
    @Builder.Default private FitPreference preferredFit = FitPreference.REGULAR;
    /** height, weight, chest, waist, hips, shoulder, neck, sleeve, inseam, thigh, footLength - in `unit`. All optional. */
    @Builder.Default private Map<String, Double> values = new LinkedHashMap<>();
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
