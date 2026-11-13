
package com.universalfit.repository;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.SizeChart;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SizeChartRepository
        extends MongoRepository<SizeChart, ObjectId> {

    List<SizeChart> findByBrandIdAndActiveTrue(ObjectId brandId);

    List<SizeChart> findByBrandId(ObjectId brandId);

    List<SizeChart> findByBrandIdAndCategoryAndActiveTrue(
            ObjectId brandId,
            Category category
    );

    List<SizeChart> findByBrandIdAndCategoryAndSizingSystemAndRegionAndMeasurementBasisAndActiveTrue(
            ObjectId brandId,
            Category category,
            SizingSystem sizingSystem,
            String region,
            MeasurementBasis measurementBasis
    );

    long countByVerificationStatus(VerificationStatus status);

    long countByBrandId(ObjectId brandId);
}
