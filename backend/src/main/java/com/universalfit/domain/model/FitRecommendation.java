package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("fit_recommendations")
@CompoundIndex(name = "user_created", def = "{'userId': 1, 'createdAt': -1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FitRecommendation {
    @Id private ObjectId id;
    private ObjectId userId;
    @Indexed private ObjectId productId;
    private ObjectId brandId;
    private Category category;
    private ObjectId sizeChartId;          // null when a product-specific spec was used
    private String recommendedSize;        // null when the engine declined to guess
    private double confidence;
    private boolean uncertain;
    private FitPreference fitType;         // preference applied
    private String reason;
    @Builder.Default private List<String> alternatives = new ArrayList<>();
    @Builder.Default private Map<String, String> measurementAnalysis = new LinkedHashMap<>();
    @Builder.Default private List<String> warnings = new ArrayList<>();
    private boolean chartVerified;
    @CreatedDate private Instant createdAt;
}
