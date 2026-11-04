package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("fit_feedback")
@CompoundIndexes({
    @CompoundIndex(name = "user_created", def = "{'userId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "brand_product", def = "{'brandId': 1, 'productId': 1}")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FitFeedback {
    @Id private ObjectId id;
    private ObjectId userId;
    private ObjectId productId;
    private ObjectId brandId;
    private ObjectId recommendationId;     // optional link
    private String recommendedSize;
    private String purchasedSize;
    private FeedbackType feedback;
    private String comments;
    @Indexed @Builder.Default private ModerationStatus moderationStatus = ModerationStatus.PENDING;
    @CreatedDate private Instant createdAt;
}
