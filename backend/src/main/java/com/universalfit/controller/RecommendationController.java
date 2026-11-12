package com.universalfit.controller;
import com.universalfit.domain.model.FitRecommendation;
import com.universalfit.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<FitRecommendation> recommendByBrandAndCategory(
            @PathVariable String brandId,
            @PathVariable com.universalfit.domain.enums.Category category,
            @RequestParam(required = false) String region,
            Authentication authentication) {
        if (!ObjectId.isValid(brandId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid brand ID");
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null || authentication.getName().isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        return ResponseEntity.ok(recommendationService.recommendByBrandAndCategory(authentication.getName(), new ObjectId(brandId), category, region));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<FitRecommendation> recommend(
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
        FitRecommendation recommendation =
                recommendationService.recommend(
                        authentication.getName(),
                        new ObjectId(productId)
                );
        return ResponseEntity.ok(recommendation);
    }
}