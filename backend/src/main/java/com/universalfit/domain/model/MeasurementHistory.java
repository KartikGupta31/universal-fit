
package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("measurement_history")
@CompoundIndex(name = "user_recorded", def = "{'userId': 1, 'recordedAt': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementHistory {

    @Id
    private ObjectId id;

    private ObjectId userId;

    private Unit unit;

    private FitPreference preferredFit;

    private Map<String, Double> values;

    // Always stored in kilograms.
    private Double weightKg;

    private Instant recordedAt;
}
