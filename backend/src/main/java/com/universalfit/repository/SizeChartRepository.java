package com.universalfit.repository;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import java.util.*;
import org.bson.types.ObjectId;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SizeChartRepository extends MongoRepository<SizeChart, ObjectId> {
    List<SizeChart> findByBrandIdAndActiveTrue(ObjectId brandId);
    List<SizeChart> findByBrandIdAndCategoryAndActiveTrue(ObjectId brandId, Category category);
    long countByVerificationStatus(VerificationStatus status);
    long countByBrandId(ObjectId brandId);
}
