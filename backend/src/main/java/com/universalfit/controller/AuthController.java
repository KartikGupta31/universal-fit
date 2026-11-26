package com.universalfit.controller;

import com.universalfit.domain.model.User;
import com.universalfit.repository.UserRepository;
import com.universalfit.security.JwtUtil;
import com.universalfit.service.GoogleIdentityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final GoogleIdentityService googleIdentityService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .build();
        userRepository.save(user);
        return tokenResponse(email);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        return tokenResponse(email);
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleRequest request) {
        var identity = googleIdentityService.verify(request.credential());
        if (identity.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Google sign-in could not be verified"));
        }

        var google = identity.get();
        String email = google.email().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email).orElseGet(() ->
                User.builder()
                        .email(email)
                        .displayName(google.name())
                        .build());

        if (!user.isEnabled()) {
            return ResponseEntity.status(403).body(Map.of("error", "This account is disabled"));
        }

        // Google has verified ownership of this email. Link to an existing account
        // without changing its password, role, or measurement-consent settings.
        if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            user.setDisplayName(google.name());
        }
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return tokenResponse(email);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        Optional<User> user = jwtUtil.validate(request.refreshToken(), JwtUtil.TokenType.REFRESH)
                .flatMap(userRepository::findByEmail)
                .filter(User::isEnabled);

        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired refresh token"));
        }

        String email = user.get().getEmail();
        var newAccess = jwtUtil.generateAccessToken(email);
        var newRefresh = jwtUtil.generateRefreshToken(email);
        return ResponseEntity.ok(Map.of("accessToken", newAccess, "refreshToken", newRefresh));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    private ResponseEntity<?> tokenResponse(String email) {
        var access = jwtUtil.generateAccessToken(email);
        var refresh = jwtUtil.generateRefreshToken(email);
        return ResponseEntity.ok(Map.of("accessToken", access, "refreshToken", refresh));
    }

    record RegisterRequest(@NotBlank @Email String email, @NotBlank String password, String displayName) {}
    record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    record RefreshRequest(@NotBlank String refreshToken) {}
    record GoogleRequest(@NotBlank String credential) {}
}
