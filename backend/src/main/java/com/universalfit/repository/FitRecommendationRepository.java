package com.universalfit.repository;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import java.util.*;
import org.bson.types.ObjectId;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FitRecommendationRepository extends MongoRepository<FitRecommendation, ObjectId> {
    Page<FitRecommendation> findByUserId(ObjectId userId, Pageable pageable);
    Optional<FitRecommendation> findFirstByUserIdAndProductIdOrderByCreatedAtDesc(ObjectId userId, ObjectId productId);
    void deleteByUserId(ObjectId userId);
}
