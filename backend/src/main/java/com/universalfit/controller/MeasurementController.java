
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static final double CM_PER_INCH = 2.54;

    /**
     * Plausible adult bounds in centimetres. MUST match PLAUSIBLE_CM in engine/app/units.py: the engine rejects
     * (HTTP 422) any saved profile value outside these bounds, so accepting a wider range here would let a user
     * save a profile that can never produce a recommendation. See contracts/README.md.
     */
    private static final Map<String, double[]> PLAUSIBLE_CM = Map.of(
            "height", new double[]{100.0, 250.0},
            "chest", new double[]{50.0, 200.0},
            "waist", new double[]{40.0, 200.0},
            "hips", new double[]{50.0, 220.0},
            "shoulder", new double[]{25.0, 80.0},
            "neck", new double[]{20.0, 70.0},
            "sleeve", new double[]{40.0, 100.0},
            "inseam", new double[]{40.0, 120.0},
            "thigh", new double[]{25.0, 100.0},
            "footLength", new double[]{15.0, 40.0}
    );

    /** Weight bounds in kilograms; same as the engine's weight check. Weight is always stored in kg. */
    private static final double MIN_WEIGHT_KG = 20.0;
    private static final double MAX_WEIGHT_KG = 300.0;

    /**
     * Validates a body measurement against the same plausible range the recommendation engine enforces.
     * Inch values are converted exactly the way the engine converts them (value * 2.54).
     */
    private boolean isValidMeasurement(
            String key, double value, Unit unit) {

        if (!Double.isFinite(value) || unit == null) {
            return false;
        }

        double[] bounds = PLAUSIBLE_CM.get(key);
        if (bounds == null) {
            return false;
        }

        double cm = unit == Unit.IN ? value * CM_PER_INCH : value;
        return cm >= bounds[0] && cm <= bounds[1];
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
                || weightKg < MIN_WEIGHT_KG
                || weightKg > MAX_WEIGHT_KG)) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Weight must be between 20 and 300 kg"
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
