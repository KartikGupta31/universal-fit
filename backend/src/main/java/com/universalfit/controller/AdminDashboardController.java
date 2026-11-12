package com.universalfit.controller;

import com.universalfit.domain.enums.FeedbackType;
import com.universalfit.domain.enums.ModerationStatus;
import com.universalfit.domain.enums.Role;
import com.universalfit.domain.model.Brand;
import com.universalfit.domain.model.FitFeedback;
import com.universalfit.domain.model.User;
import com.universalfit.repository.BrandRepository;
import com.universalfit.repository.FitFeedbackRepository;
import com.universalfit.repository.FitRecommendationRepository;
import com.universalfit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final UserRepository userRepository;
    private final FitRecommendationRepository recommendationRepository;
    private final FitFeedbackRepository feedbackRepository;
    private final BrandRepository brandRepository;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(Authentication authentication) {
        requireAdmin(authentication);
        Map<String, Long> feedbackBreakdown = new EnumMap<>(FeedbackType.class);
        for (FeedbackType type : FeedbackType.values()) {
            feedbackBreakdown.put(type.name(), feedbackRepository.countByFeedback(type));
        }
        List<FitFeedback> latest = feedbackRepository.findAll(
                PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        var brandIds = latest.stream().map(FitFeedback::getBrandId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        var brands = brandRepository.findAllById(brandIds).stream()
                .collect(Collectors.toMap(Brand::getId, Brand::getName));
        List<FeedbackItem> recentFeedback = latest.stream().map(item -> new FeedbackItem(
                item.getId().toHexString(),
                item.getBrandId() == null ? "Unknown brand" : brands.getOrDefault(item.getBrandId(), "Unknown brand"),
                item.getFeedback().name(), item.getRecommendedSize(), item.getPurchasedSize(),
                item.getComments(), item.getModerationStatus().name(), item.getCreatedAt()
        )).toList();
        return new DashboardResponse(
                userRepository.count(),
                recommendationRepository.count(),
                feedbackRepository.count(),
                feedbackRepository.countByModerationStatus(ModerationStatus.PENDING),
                feedbackRepository.countByModerationStatus(ModerationStatus.APPROVED),
                feedbackRepository.countByModerationStatus(ModerationStatus.REJECTED),
                brandRepository.countByActiveTrue(),
                feedbackBreakdown,
                recentFeedback
        );
    }

    @PatchMapping("/feedback/{feedbackId}/moderation")
    public FeedbackItem moderateFeedback(@PathVariable String feedbackId,
                                         @RequestBody ModerationRequest request,
                                         Authentication authentication) {
        requireAdmin(authentication);
        if (!ObjectId.isValid(feedbackId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feedback ID");
        if (request == null || request.status() == null || request.status() == ModerationStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose APPROVED or REJECTED");
        FitFeedback feedback = feedbackRepository.findById(new ObjectId(feedbackId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback not found"));
        feedback.setModerationStatus(request.status());
        FitFeedback saved = feedbackRepository.save(feedback);
        String brandName = saved.getBrandId() == null ? "Unknown brand" :
                brandRepository.findById(saved.getBrandId()).map(Brand::getName).orElse("Unknown brand");
        return new FeedbackItem(saved.getId().toHexString(), brandName, saved.getFeedback().name(),
                saved.getRecommendedSize(), saved.getPurchasedSize(), saved.getComments(),
                saved.getModerationStatus().name(), saved.getCreatedAt());
    }

    private User requireAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isEnabled()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account disabled");
        if (user.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
        return user;
    }

    public record DashboardResponse(long totalUsers, long totalRecommendations, long totalFeedback,
                                    long pendingFeedback, long approvedFeedback, long rejectedFeedback,
                                    long activeBrands, Map<String, Long> feedbackBreakdown,
                                    List<FeedbackItem> recentFeedback) {}
    public record FeedbackItem(String id, String brandName, String feedback, String recommendedSize,
                               String purchasedSize, String comments, String moderationStatus, Instant createdAt) {}
    public record ModerationRequest(ModerationStatus status) {}
}