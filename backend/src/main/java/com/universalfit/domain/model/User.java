package com.universalfit.domain.model;

import com.universalfit.domain.enums.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

/** Account. Passwords are stored only as BCrypt hashes and never serialised to API responses. */
@Document("users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id private ObjectId id;
    @Indexed(unique = true) private String email;       // stored lower-case
    private String passwordHash;
    private String displayName;
    @Builder.Default private Role role = Role.USER;
    @Builder.Default private boolean enabled = true;
    private boolean measurementConsent;                 // explicit consent to store body measurements
    private Instant measurementConsentAt;
    private Instant lastLoginAt;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
