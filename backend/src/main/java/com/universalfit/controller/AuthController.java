package com.universalfit.controller;

import com.universalfit.domain.model.User;
import com.universalfit.repository.UserRepository;
import com.universalfit.security.JwtUtil;
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

import java.util.Map;

/**
 * Authentication endpoints for registration, login, token refresh and logout.
 * All responses contain an {@code accessToken} and a {@code refreshToken} where applicable.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .build();
        userRepository.save(user);
        var access = jwtUtil.generateAccessToken(email);
        var refresh = jwtUtil.generateRefreshToken(email);
        return ResponseEntity.ok(Map.of("accessToken", access, "refreshToken", refresh));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        String email = request.email().toLowerCase();
        var access = jwtUtil.generateAccessToken(email);
        var refresh = jwtUtil.generateRefreshToken(email);
        return ResponseEntity.ok(Map.of("accessToken", access, "refreshToken", refresh));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        String token = request.refreshToken();
        // Validate refresh token (signature + expiration). Username is extracted for new tokens.
        try {
            String username = jwtUtil.extractUsername(token);
            if (!jwtUtil.isTokenValid(token, username)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired refresh token"));
            }
            var newAccess = jwtUtil.generateAccessToken(username);
            var newRefresh = jwtUtil.generateRefreshToken(username); // rotation
            return ResponseEntity.ok(Map.of("accessToken", newAccess, "refreshToken", newRefresh));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Stateless JWT – nothing to do server‑side. Clients should discard tokens.
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    // DTOs -------------------------------------------------------------
    record RegisterRequest(@Email String email,
                          @NotBlank String password,
                          String displayName) {}

    record LoginRequest(@Email String email,
                        @NotBlank String password) {}

    record RefreshRequest(@NotBlank String refreshToken) {}
}
