package com.universalfit.repository;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import java.util.*;
import org.bson.types.ObjectId;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SavedProductRepository extends MongoRepository<SavedProduct, ObjectId> {
    Page<SavedProduct> findByUserId(ObjectId userId, Pageable pageable);
    Optional<SavedProduct> findByIdAndUserId(ObjectId id, ObjectId userId);
    boolean existsByUserIdAndProductId(ObjectId userId, ObjectId productId);
    void deleteByUserId(ObjectId userId);
}
