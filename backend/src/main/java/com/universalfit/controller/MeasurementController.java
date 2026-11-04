
package com.universalfit.controller;

import com.universalfit.domain.enums.FitPreference;
import com.universalfit.domain.enums.Unit;
import com.universalfit.domain.model.MeasurementHistory;
import com.universalfit.domain.model.MeasurementProfile;
import com.universalfit.domain.model.User;
import com.universalfit.repository.MeasurementHistoryRepository;
import com.universalfit.repository.MeasurementProfileRepository;
import com.universalfit.repository.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/measurements")
@RequiredArgsConstructor
public class MeasurementController {

    private final UserRepository userRepository;
    private final MeasurementProfileRepository profileRepository;
    private final MeasurementHistoryRepository historyRepository;

    private static final Set<String> ALLOWED_MEASUREMENTS = Set.of(
            "height", "chest", "waist", "hips",
            "shoulder", "neck", "sleeve",
            "inseam", "thigh", "footLength"
    );

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    /**
     * Validates body measurements according to the selected unit.
     * These are broad input sanity limits, not fit recommendations.
     */
    private boolean isValidMeasurement(
            String key, double value, Unit unit) {

        if (!Double.isFinite(value) || unit == null) {
            return false;
        }

        double min;
        double max;

        switch (key) {
            case "height" -> {
                min = unit == Unit.CM ? 50 : 20;
                max = unit == Unit.CM ? 250 : 100;
            }
            case "chest", "waist", "hips" -> {
                min = unit == Unit.CM ? 30 : 12;
                max = unit == Unit.CM ? 200 : 80;
            }
            case "shoulder", "neck" -> {
                min = unit == Unit.CM ? 10 : 4;
                max = unit == Unit.CM ? 100 : 40;
            }
            case "sleeve", "inseam" -> {
                min = unit == Unit.CM ? 10 : 4;
                max = unit == Unit.CM ? 150 : 60;
            }
            case "thigh" -> {
                min = unit == Unit.CM ? 15 : 6;
                max = unit == Unit.CM ? 120 : 48;
            }
            case "footLength" -> {
                min = unit == Unit.CM ? 5 : 2;
                max = unit == Unit.CM ? 50 : 20;
            }
            default -> {
                return false;
            }
        }

        return value >= min && value <= max;
    }

    // GET: Retrieve current user's measurement profile
    @GetMapping("/me")
    public ResponseEntity<?> getMyMeasurements(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        if (!user.isMeasurementConsent()) {
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "message",
                            "Measurement consent is required"
                    ));
        }

        return profileRepository.findByUserId(user.getId())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // PUT: Save or update measurement profile
    @PutMapping("/me")
    public ResponseEntity<?> saveMeasurements(
            Authentication authentication,
            @Valid @RequestBody MeasurementRequest request) {

        User user = getCurrentUser(authentication);

        if (!user.isMeasurementConsent()) {
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "message",
                            "Measurement consent is required"
                    ));
        }

        if (request.values() == null || request.values().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "At least one body measurement is required"
                    ));
        }

        Unit requestedUnit = request.unit() != null
                ? request.unit()
                : Unit.CM;

        // Validate body measurements
        Map<String, Double> values = new LinkedHashMap<>();

        for (Map.Entry<String, Double> entry
                : request.values().entrySet()) {

            String key = entry.getKey();
            Double value = entry.getValue();

            if (!ALLOWED_MEASUREMENTS.contains(key)) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "message",
                                "Invalid measurement: " + key
                        ));
            }

            if (value == null
                    || !isValidMeasurement(key, value, requestedUnit)) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "message",
                                "Invalid value for " + key
                                        + " in " + requestedUnit
                        ));
            }

            values.put(key, value);
        }

        // Validate weight separately. Always in kilograms.
        Double weightKg = request.weightKg();

        if (weightKg != null
                && (!Double.isFinite(weightKg)
                || weightKg < 1
                || weightKg > 500)) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Weight must be between 1 and 500 kg"
                    ));
        }

        MeasurementProfile profile = profileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> MeasurementProfile.builder()
                        .userId(user.getId())
                        .build());

        profile.setUnit(requestedUnit);

        profile.setPreferredFit(
                request.preferredFit() != null
                        ? request.preferredFit()
                        : FitPreference.REGULAR
        );

        profile.setValues(values);

        // Preserve previously saved weight if omitted.
        if (weightKg != null) {
            profile.setWeightKg(weightKg);
        }

        MeasurementProfile saved =
                profileRepository.save(profile);

        // Save historical snapshot
        MeasurementHistory snapshot =
                MeasurementHistory.builder()
                        .userId(user.getId())
                        .unit(saved.getUnit())
                        .preferredFit(saved.getPreferredFit())
                        .values(new LinkedHashMap<>(
                                saved.getValues()
                        ))
                        .weightKg(saved.getWeightKg())
                        .recordedAt(Instant.now())
                        .build();

        historyRepository.save(snapshot);

        return ResponseEntity.ok(saved);
    }

    // DELETE: Delete current user's profile and history
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyMeasurements(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        profileRepository.deleteByUserId(user.getId());
        historyRepository.deleteByUserId(user.getId());

        return ResponseEntity.ok(Map.of(
                "message",
                "Measurement profile and history deleted"
        ));
    }

    // GET: Retrieve measurement history with pagination
    @GetMapping("/history")
    public ResponseEntity<?> getMeasurementHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = getCurrentUser(authentication);

        if (!user.isMeasurementConsent()) {
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "message",
                            "Measurement consent is required"
                    ));
        }

        if (page < 0 || size < 1 || size > 50) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid pagination parameters"
                    ));
        }

        Page<MeasurementHistory> history =
                historyRepository.findByUserId(
                        user.getId(),
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        Sort.Direction.DESC,
                                        "recordedAt"
                                )
                        )
                );

        return ResponseEntity.ok(history);
    }

    // Request DTO
    public record MeasurementRequest(
            Unit unit,
            FitPreference preferredFit,
            Double weightKg,
            @NotNull Map<String, Double> values
    ) {}
}
