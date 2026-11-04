package com.universalfit.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.function.Function;

/**
 * Utility class for creating and validating JWT access and refresh tokens.
 * All secrets and TTL values are read from the application configuration (environment variables).
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${universalfit.jwt.secret:}")
    private String secretString;

    @Value("${universalfit.jwt.access-ttl-minutes:15}")
    private long accessTtlMinutes;

    @Value("${universalfit.jwt.refresh-ttl-days:14}")
    private long refreshTtlDays;

    private javax.crypto.SecretKey key;

    @PostConstruct
    private void init() {
        if (secretString == null || secretString.isBlank()) {
            log.error("JWT secret is not configured – authentication will not work");
            throw new IllegalStateException("JWT secret must be provided via JWT_SECRET env variable");
        }
        // JJWT expects a byte array; using HMAC SHA256.
        this.key = Keys.hmacShaKeyFor(secretString.getBytes());
    }

    // ----- token creation -------------------------------------------------
    public String generateAccessToken(String subject) {
        return buildToken(subject, Duration.ofMinutes(accessTtlMinutes));
    }

    public String generateRefreshToken(String subject) {
        return buildToken(subject, Duration.ofDays(refreshTtlDays));
    }

    private String buildToken(String subject, Duration ttl) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttl.toMillis());
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ----- token validation ------------------------------------------------
    public boolean isTokenValid(String token, String userDetailsUsername) {
        try {
            String username = extractUsername(token);
            return username.equalsIgnoreCase(userDetailsUsername) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }
}
