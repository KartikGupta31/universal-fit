package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/** A fashion brand. Sizing lives in SizeChart, never on the brand itself. */
@Document("brands")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Brand {
    @Id
    @JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private ObjectId id;
    @Indexed(unique = true) private String slug;
    @TextIndexed private String name;
    private String logoUrl;
    private String country;
    @Builder.Default private List<String> supportedRegions = new ArrayList<>();
    @Indexed @Builder.Default private List<Category> categories = new ArrayList<>();
    private String dataSource;
    @Builder.Default private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    private Instant lastVerifiedAt;
    private String notes;
    @Indexed @Builder.Default private boolean active = true;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
