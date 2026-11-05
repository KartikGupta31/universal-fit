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
import java.util.Optional;

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
        // Only a valid REFRESH token is accepted (an access token is rejected), and the user must still
        // exist and be enabled. One generic message for every failure, so nothing is revealed about why.
        Optional<User> user = jwtUtil.validate(request.refreshToken(), JwtUtil.TokenType.REFRESH)
                .flatMap(userRepository::findByEmail)
                .filter(User::isEnabled);

        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired refresh token"));
        }

        String email = user.get().getEmail();
        var newAccess = jwtUtil.generateAccessToken(email);
        var newRefresh = jwtUtil.generateRefreshToken(email); // rotation
        return ResponseEntity.ok(Map.of("accessToken", newAccess, "refreshToken", newRefresh));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Stateless JWT – nothing to do server‑side. Clients should discard tokens.
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    // DTOs -------------------------------------------------------------
    record RegisterRequest(@NotBlank @Email String email,
                          @NotBlank String password,
                          String displayName) {}

    record LoginRequest(@NotBlank @Email String email,
                        @NotBlank String password) {}

    record RefreshRequest(@NotBlank String refreshToken) {}
}
