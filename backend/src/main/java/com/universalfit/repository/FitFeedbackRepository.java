package com.universalfit.repository;

import com.universalfit.domain.enums.*;
import com.universalfit.domain.model.*;
import java.util.*;
import org.bson.types.ObjectId;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FitFeedbackRepository extends MongoRepository<FitFeedback, ObjectId> {
    Page<FitFeedback> findByUserId(ObjectId userId, Pageable pageable);
    Page<FitFeedback> findByModerationStatus(ModerationStatus status, Pageable pageable);
    long countByFeedback(FeedbackType type);
    void deleteByUserId(ObjectId userId);
}
