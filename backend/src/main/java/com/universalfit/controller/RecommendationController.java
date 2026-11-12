package com.universalfit.controller;
import com.universalfit.domain.model.FitRecommendation;
import com.universalfit.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;

    @PostMapping("/brand/{brandId}/category/{category}")
    public ResponseEntity<RecommendationResponse> recommendByBrandAndCategory(
            @PathVariable String brandId,
            @PathVariable com.universalfit.domain.enums.Category category,
            @RequestParam(required = false) String region,
            @RequestParam com.universalfit.domain.enums.SizingSystem sizingSystem,
            Authentication authentication) {
        if (!ObjectId.isValid(brandId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid brand ID");
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null || authentication.getName().isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        return ResponseEntity.ok(RecommendationResponse.from(recommendationService.recommendByBrandAndCategory(authentication.getName(), new ObjectId(brandId), category, region, sizingSystem)));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<RecommendationResponse> recommend(
            @PathVariable String productId,
            Authentication authentication
    ) {
        if (!ObjectId.isValid(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid product ID"
            );
        }
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }
        FitRecommendation recommendation = recommendationService.recommend(authentication.getName(), new ObjectId(productId));
        return ResponseEntity.ok(RecommendationResponse.from(recommendation));
    }
    public record RecommendationResponse(
            String id,
            String recommendedSize,
            double confidence,
            boolean uncertain,
            com.universalfit.domain.enums.FitPreference fitType,
            String reason,
            List<String> alternatives,
            Map<String, String> measurementAnalysis,
            List<String> warnings,
            boolean chartVerified,
            Instant createdAt) {
        static RecommendationResponse from(FitRecommendation item) {
            return new RecommendationResponse(
                    item.getId() == null ? null : item.getId().toHexString(),
                    item.getRecommendedSize(), item.getConfidence(), item.isUncertain(),
                    item.getFitType(), item.getReason(), item.getAlternatives(),
                    item.getMeasurementAnalysis(), item.getWarnings(),
                    item.isChartVerified(), item.getCreatedAt());
        }
    }
}
}
