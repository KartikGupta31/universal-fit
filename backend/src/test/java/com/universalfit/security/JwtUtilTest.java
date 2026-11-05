package com.universalfit.security;

import static com.universalfit.security.JwtUtil.TokenType.ACCESS;
import static com.universalfit.security.JwtUtil.TokenType.REFRESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.Test;

/** Plain unit test: no Spring, no MongoDB, no Docker. Safe to run anywhere. */
class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-at-least-32-bytes-long-0123456789";
    private static final String EMAIL = "user@example.com";

    private final JwtUtil jwt = new JwtUtil(SECRET, 15, 14);

    private static String base64Url(String text) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void accessTokenValidatesAsAccessAndReturnsSubject() {
        assertThat(jwt.validate(jwt.generateAccessToken(EMAIL), ACCESS)).contains(EMAIL);
    }

    @Test
    void refreshTokenValidatesAsRefreshAndReturnsSubject() {
        assertThat(jwt.validate(jwt.generateRefreshToken(EMAIL), REFRESH)).contains(EMAIL);
    }

    @Test
    void refreshTokenIsRejectedWhereAnAccessTokenIsExpected() {
        assertThat(jwt.validate(jwt.generateRefreshToken(EMAIL), ACCESS)).isEmpty();
    }

    @Test
    void accessTokenIsRejectedWhereARefreshTokenIsExpected() {
        assertThat(jwt.validate(jwt.generateAccessToken(EMAIL), REFRESH)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        String expiredAccessToken = Jwts.builder()
                .subject(EMAIL)
                .claim(JwtUtil.TOKEN_TYPE_CLAIM, "access")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(
                        Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)),
                        Jwts.SIG.HS256)
                .compact();

        String expiredRefreshToken = Jwts.builder()
                .subject(EMAIL)
                .claim(JwtUtil.TOKEN_TYPE_CLAIM, "refresh")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(
                        Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)),
                        Jwts.SIG.HS256)
                .compact();

        assertThat(jwt.validate(expiredAccessToken, ACCESS)).isEmpty();
        assertThat(jwt.validate(expiredRefreshToken, REFRESH)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtUtil other = new JwtUtil("a-completely-different-secret-32-bytes-or-more!!", 15, 14);
        assertThat(jwt.validate(other.generateAccessToken(EMAIL), ACCESS)).isEmpty();
    }

    @Test
    void tamperedPayloadIsRejected() {
        String[] parts = jwt.generateAccessToken(EMAIL).split("\\.");
        String forgedPayload = base64Url(
                "{\"sub\":\"admin@example.com\",\"token_type\":\"access\",\"exp\":4102444800}");
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];
        assertThat(jwt.validate(forged, ACCESS)).isEmpty();
    }

    @Test
    void unsignedTokenIsRejected() {
        String unsigned = base64Url("{\"alg\":\"none\"}") + "."
                + base64Url("{\"sub\":\"" + EMAIL + "\",\"token_type\":\"access\",\"exp\":4102444800}") + ".";
        assertThat(jwt.validate(unsigned, ACCESS)).isEmpty();
    }

    @Test
    void validlySignedTokenWithoutTypeClaimIsRejected() {
        String legacy = Jwts.builder()
                .subject(EMAIL)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertThat(jwt.validate(legacy, ACCESS)).isEmpty();
        assertThat(jwt.validate(legacy, REFRESH)).isEmpty();
    }

    @Test
    void garbageNullAndBlankNeverThrow() {
        assertThat(jwt.validate("not-a-jwt", ACCESS)).isEmpty();
        assertThat(jwt.validate("a.b.c", ACCESS)).isEmpty();
        assertThat(jwt.validate("", ACCESS)).isEmpty();
        assertThat(jwt.validate("   ", ACCESS)).isEmpty();
        assertThat(jwt.validate(null, ACCESS)).isEmpty();
    }

    @Test
    void nullExpectedTokenTypeIsRejected() {
        assertThat(jwt.validate(jwt.generateAccessToken(EMAIL), null)).isEmpty();
    }

    @Test
    void zeroOrNegativeTokenLifetimeFailsAtStartup() {
        assertThatThrownBy(() -> new JwtUtil(SECRET, 0, 14))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> new JwtUtil(SECRET, 15, 0))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> new JwtUtil(SECRET, -1, 14))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingOrWeakSecretFailsAtStartup() {
        assertThatThrownBy(() -> new JwtUtil("", 15, 14))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> new JwtUtil(null, 15, 14))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> new JwtUtil("too-short", 15, 14))
                .isInstanceOf(WeakKeyException.class);
    }
}
