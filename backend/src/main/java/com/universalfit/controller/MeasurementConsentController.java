
package com.universalfit.controller;

import com.universalfit.domain.model.User;
import com.universalfit.repository.UserRepository;
import com.universalfit.repository.MeasurementProfileRepository;
import com.universalfit.repository.MeasurementHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/measurements")
@RequiredArgsConstructor
public class MeasurementConsentController {

    private final UserRepository userRepository;
    private final MeasurementProfileRepository profileRepository;
    private final MeasurementHistoryRepository historyRepository;

    @PutMapping("/consent")
    public ResponseEntity<?> updateConsent(
            Authentication authentication,
            @RequestBody ConsentRequest request) {

        // Safety check FIRST, before touching any data: revoking consent deletes measurements, so the
        // client must say so explicitly. A missing or null "consent" is a bad request, never "false".
        if (request == null || request.consent() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "The 'consent' field is required and must be true or false"));
        }
        boolean granted = request.consent();

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setMeasurementConsent(granted);
        user.setMeasurementConsentAt(granted ? Instant.now() : null);

        userRepository.save(user);

        // Revoking consent also deletes current measurements and history.
        if (!granted) {
            profileRepository.deleteByUserId(user.getId());
            historyRepository.deleteByUserId(user.getId());
        }

        return ResponseEntity.ok(Map.of(
                "measurementConsent", granted,
                "message", granted
                        ? "Measurement consent granted"
                        : "Measurement consent revoked and measurement data deleted"
        ));
    }

    /** {@code Boolean} (not {@code boolean}) on purpose: a missing field must stay null, not become false. */
    public record ConsentRequest(Boolean consent) {}
}
