package com.universalfit.repository;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import java.util.*;
import org.bson.types.ObjectId;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, ObjectId> {
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByActiveTrueAndBrandId(ObjectId brandId, Pageable pageable);
    Page<Product> findByActiveTrueAndCategory(Category category, Pageable pageable);
    Page<Product> findByActiveTrueAndBrandIdAndCategory(ObjectId brandId, Category category, Pageable pageable);
    List<Product> findByActiveTrueAndBrandIdAndCategory(ObjectId brandId, Category category);
    long countByActiveTrue();
    long countByBrandId(ObjectId brandId);
}
