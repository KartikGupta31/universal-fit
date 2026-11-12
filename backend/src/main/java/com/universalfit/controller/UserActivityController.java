package com.universalfit.controller;

import com.universalfit.domain.enums.FeedbackType;
import com.universalfit.domain.model.FitFeedback;
import com.universalfit.domain.model.FitRecommendation;
import com.universalfit.domain.model.Brand;
import com.universalfit.domain.model.User;
import com.universalfit.repository.FitFeedbackRepository;
import com.universalfit.repository.FitRecommendationRepository;
import com.universalfit.repository.BrandRepository;
import com.universalfit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class UserActivityController {
    private final UserRepository userRepository;
    private final FitRecommendationRepository recommendationRepository;
    private final BrandRepository brandRepository;
    private final FitFeedbackRepository feedbackRepository;

    @GetMapping("/api/recommendations/history")
    public ResponseEntity<?> history(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     Authentication authentication) {
        User user = requireUser(authentication);
        validatePage(page, size);
        Page<FitRecommendation> results = recommendationRepository.findByUserId(
                user.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        var brandIds = results.getContent().stream().map(FitRecommendation::getBrandId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        var brandNames = brandRepository.findAllById(brandIds).stream()
                .collect(java.util.stream.Collectors.toMap(Brand::getId, Brand::getName));
        return ResponseEntity.ok(results.map(item -> new HistoryResponse(
                item.getId().toHexString(),
                item.getBrandId() == null ? "Unknown brand" : brandNames.getOrDefault(item.getBrandId(), "Unknown brand"),
                item.getCategory(), item.getRecommendedSize(), item.getConfidence(), item.isUncertain(),
                item.getReason(), item.getChartVerified(), item.getCreatedAt())));
    }

    @PostMapping("/api/feedback")
    public ResponseEntity<FeedbackResponse> submitFeedback(@RequestBody FeedbackRequest request,
                                                             Authentication authentication) {
        User user = requireUser(authentication);
        if (request == null || request.recommendationId() == null || !ObjectId.isValid(request.recommendationId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid recommendation ID is required");
        if (request.feedback() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select your fit feedback");
        FitRecommendation recommendation = recommendationRepository.findById(new ObjectId(request.recommendationId()))
                .filter(item -> user.getId().equals(item.getUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recommendation not found"));
        String comments = request.comments() == null ? null : request.comments().trim();
        String purchasedSize = request.purchasedSize() == null ? null : request.purchasedSize().trim();
        if (comments != null && comments.length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comments must be 500 characters or fewer");
        if (purchasedSize != null && purchasedSize.length() > 30)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purchased size must be 30 characters or fewer");
        if (purchasedSize != null && purchasedSize.isBlank()) purchasedSize = null;
        FitFeedback saved = feedbackRepository.save(FitFeedback.builder()
                .userId(user.getId()).productId(recommendation.getProductId()).brandId(recommendation.getBrandId())
                .recommendationId(recommendation.getId()).recommendedSize(recommendation.getRecommendedSize())
                .purchasedSize(purchasedSize).feedback(request.feedback()).comments(comments).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackResponse.from(saved));
    }

    @GetMapping("/api/feedback")
    public ResponseEntity<?> feedback(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      Authentication authentication) {
        User user = requireUser(authentication);
        validatePage(page, size);
        return ResponseEntity.ok(feedbackRepository.findByUserId(user.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).map(FeedbackResponse::from));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isEnabled()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is disabled");
        return user;
    }

    public record HistoryResponse(String id, String brandName,
                                  com.universalfit.domain.enums.Category category, String recommendedSize,
                                  double confidence, boolean uncertain, String reason,
                                  boolean chartVerified, java.time.Instant createdAt) {}
    public record FeedbackRequest(String recommendationId, FeedbackType feedback, String purchasedSize, String comments) {}
    public record FeedbackResponse(String id, String recommendationId, FeedbackType feedback,
                                   String recommendedSize, String purchasedSize, String comments,
                                   java.time.Instant createdAt) {
        static FeedbackResponse from(FitFeedback item) {
            return new FeedbackResponse(item.getId().toHexString(),
                    item.getRecommendationId() == null ? null : item.getRecommendationId().toHexString(),
                    item.getFeedback(), item.getRecommendedSize(), item.getPurchasedSize(),
                    item.getComments(), item.getCreatedAt());
        }
    }
}
