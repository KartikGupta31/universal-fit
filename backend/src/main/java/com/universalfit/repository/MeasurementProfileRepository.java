package com.universalfit.repository;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import java.util.*;
import org.bson.types.ObjectId;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MeasurementProfileRepository extends MongoRepository<MeasurementProfile, ObjectId> {
    Optional<MeasurementProfile> findByUserId(ObjectId userId);
    void deleteByUserId(ObjectId userId);
}
