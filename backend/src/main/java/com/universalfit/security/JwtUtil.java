
package com.universalfit.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Creates and validates JWTs.
 *
 * <p>Every token carries a {@code token_type} claim ({@code access} or {@code refresh}). A token is only ever
 * accepted for the purpose it was issued for: protected APIs accept ACCESS tokens only, and
 * {@code /auth/refresh} accepts REFRESH tokens only. All reading of tokens goes through
 * {@link #validate(String, TokenType)}, which never throws and returns the subject (the user's email) only
 * when the signature, expiry and type are all correct.
 *
 * <p>The secret and lifetimes come from configuration (environment variables), never from code.
 */
@Slf4j
@Component
public class JwtUtil {

    /** The purpose a token was issued for. */
    public enum TokenType {
        ACCESS("access"),
        REFRESH("refresh");

        private final String claimValue;

        TokenType(String claimValue) {
            this.claimValue = claimValue;
        }

        String claimValue() {
            return claimValue;
        }
    }

    static final String TOKEN_TYPE_CLAIM = "token_type";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtUtil(@Value("${universalfit.jwt.secret:}") String secret,
                   @Value("${universalfit.jwt.access-ttl-minutes:15}") long accessTtlMinutes,
                   @Value("${universalfit.jwt.refresh-ttl-days:14}") long refreshTtlDays) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret must be provided via the JWT_SECRET environment variable");
        }

        // Throws WeakKeyException if the secret is shorter than 32 bytes.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        this.accessTtl = Duration.ofMinutes(accessTtlMinutes);
        this.refreshTtl = Duration.ofDays(refreshTtlDays);

        // Prevent invalid token expiration configurations.
        if (accessTtl.isZero() || accessTtl.isNegative()
                || refreshTtl.isZero() || refreshTtl.isNegative()) {
            throw new IllegalStateException("JWT token lifetimes must be positive");
        }
    }

    // ----- token creation -------------------------------------------------

    public String generateAccessToken(String subject) {
        return buildToken(subject, TokenType.ACCESS, accessTtl);
    }

    public String generateRefreshToken(String subject) {
        return buildToken(subject, TokenType.REFRESH, refreshTtl);
    }

    private String buildToken(String subject, TokenType type, Duration ttl) {
        Date now = new Date();

        return Jwts.builder()
                .subject(subject)
                .claim(TOKEN_TYPE_CLAIM, type.claimValue())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // ----- token validation ------------------------------------------------

    /**
     * Checks signature, expiry and token type.
     *
     * @return the subject (user email) if the token is valid AND of the expected type;
     *         otherwise empty.
     *         Never throws for invalid token input.
     */
    public Optional<String> validate(String token, TokenType expected) {

        // Reject null/blank tokens and null expected token types.
        if (token == null || token.isBlank() || expected == null) {
            return Optional.empty();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String actualType = claims.get(TOKEN_TYPE_CLAIM, String.class);

            if (!expected.claimValue().equals(actualType)) {
                log.debug("Rejected JWT: expected {} token but got '{}'", expected, actualType);
                return Optional.empty();
            }

            return Optional.ofNullable(claims.getSubject())
                    .filter(subject -> !subject.isBlank());

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
