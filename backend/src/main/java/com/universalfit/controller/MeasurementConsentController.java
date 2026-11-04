
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

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setMeasurementConsent(request.consent());
        user.setMeasurementConsentAt(
                request.consent() ? Instant.now() : null
        );

        userRepository.save(user);

        // Revoking consent also deletes current measurements and history.
        if (!request.consent()) {
            profileRepository.deleteByUserId(user.getId());
            historyRepository.deleteByUserId(user.getId());
        }

        return ResponseEntity.ok(Map.of(
                "measurementConsent", user.isMeasurementConsent(),
                "message", request.consent()
                        ? "Measurement consent granted"
                        : "Measurement consent revoked and measurement data deleted"
        ));
    }

    public record ConsentRequest(boolean consent) {}
}
