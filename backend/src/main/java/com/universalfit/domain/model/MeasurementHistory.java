package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

/** Immutable snapshot written every time the measurement profile changes. */
@Document("measurement_history")
@CompoundIndex(name = "user_recorded", def = "{'userId': 1, 'recordedAt': -1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeasurementHistory {
    @Id private ObjectId id;
    private ObjectId userId;
    private Unit unit;
    private FitPreference preferredFit;
    private Map<String, Double> values;
    private Instant recordedAt;
}
