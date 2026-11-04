package com.universalfit.repository;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import java.util.*;
import org.bson.types.ObjectId;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BrandRepository extends MongoRepository<Brand, ObjectId> {
    Optional<Brand> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Brand> findByActiveTrue(Pageable pageable);
    Page<Brand> findByActiveTrueAndCategoriesContaining(Category category, Pageable pageable);
    Page<Brand> findByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);
    long countByActiveTrue();
}
