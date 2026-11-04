
package com.universalfit.controller;

import com.universalfit.domain.model.User;
import com.universalfit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(
                        new UserProfileResponse(
                                user.getEmail(),
                                user.getDisplayName(),
                                user.getRole().name(),
                                user.isMeasurementConsent(),
                                user.getCreatedAt()
                        )
                ))
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found"));
    }

    public record UserProfileResponse(
            String email,
            String displayName,
            String role,
            boolean measurementConsent,
            Instant createdAt
    ) {}
}
