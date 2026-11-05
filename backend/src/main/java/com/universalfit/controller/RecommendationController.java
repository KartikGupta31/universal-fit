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