package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("saved_products")
@CompoundIndex(name = "user_product", def = "{'userId': 1, 'productId': 1}", unique = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedProduct {
    @Id private ObjectId id;
    private ObjectId userId;
    private ObjectId productId;
    private ObjectId lastRecommendationId;
    @CreatedDate private Instant createdAt;
}
