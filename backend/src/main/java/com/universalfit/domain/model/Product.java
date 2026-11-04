package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
@CompoundIndex(name = "product_browse", def = "{'brandId': 1, 'category': 1, 'active': 1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id private ObjectId id;
    @TextIndexed private String name;
    @Indexed private ObjectId brandId;
    @Indexed private Category category;
    private SizingSystem sizingSystem;
    @TextIndexed private String description;
    private String imageUrl;
    private String productUrl;
    @Builder.Default private List<String> availableSizes = new ArrayList<>();
    @Builder.Default private FitPreference fitType = FitPreference.REGULAR;
    private Double price;
    private String currency;
    private String region;
    private String dataSource;             // admin entry, authorised feed, ...
    @Builder.Default private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    @Builder.Default private boolean active = true;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
