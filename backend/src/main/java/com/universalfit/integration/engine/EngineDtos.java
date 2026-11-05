package com.universalfit.integration.engine;
import com.universalfit.domain.enums.Category;
import com.universalfit.domain.enums.FitPreference;
import com.universalfit.domain.enums.MeasurementBasis;
import com.universalfit.domain.enums.Unit;
import java.util.List;
import java.util.Map;
public final class EngineDtos {
    private EngineDtos() {}
    public record Profile(
            Unit unit,
            FitPreference preferredFit,
            Double height,
            Double weight,
            Double chest,
            Double waist,
            Double hips,
            Double shoulder,
            Double neck,
            Double sleeve,
            Double inseam,
            Double thigh,
            Double footLength
    ) {}
    public record Size(String label, Map<String, Measurement> measurements) {}
    public record Measurement(double min, Double max) {}
    public record RecommendRequest(
            Profile profile,
            Category category,
            FitPreference productFitType,
            MeasurementBasis measurementBasis,
            Unit unit,
            boolean garmentFlat,
            boolean chartVerified,
            String brandName,
            List<Size> sizes
    ) {}
    public record RecommendationResponse(
            String recommendedSize,
            double confidence,
            boolean uncertain,
            FitPreference fitType,
            FitPreference productFitType,
            String reason,
            List<String> alternatives,
            Map<String, String> measurementAnalysis,
            Map<String, Double> sizeScores,
            List<String> measurementsUsed,
            List<String> missingMeasurements,
            List<String> warnings,
            Map<String, Object> dataQuality
    ) {}
}
