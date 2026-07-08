package com.security;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;

import com.exception.InvalidTokenException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    // Same dev-only placeholder already committed in application.properties (jwt.secret default) —
    // not a real secret, just a base64-encoded 256-bit key satisfying HS256's minimum key length.
    private static final String SECRET = "uF3H1w6KEkycZgLeVBuqT2GtM8Rw3jiYKyRmxUTTCts=";
    private static final long EXPIRATION_MS = 3_600_000;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_withTenantId_roundTripsBackToSameSubject() {
        UUID tenantId = UUID.randomUUID();

        String token = jwtUtil.generateToken(tenantId);

        assertThat(jwtUtil.parseSubject(token)).isEqualTo(tenantId.toString());
    }

    @Test
    void generateToken_withSubjectAndClaims_roundTripsBackToSameClaims() {
        String subject = UUID.randomUUID().toString();

        String token = jwtUtil.generateToken(subject, Map.of("tenantId", "t-1", "role", "ADMIN"));

        Claims claims = jwtUtil.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo(subject);
        assertThat(claims.get("tenantId", String.class)).isEqualTo("t-1");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void generateToken_setsIssuedAtAndExpirationExpirationMsApart() {
        String token = jwtUtil.generateToken(UUID.randomUUID());

        Claims claims = jwtUtil.parseClaims(token);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        long actualGapMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(actualGapMs).isEqualTo(EXPIRATION_MS);
    }

    @Test
    void parseClaims_throwsInvalidTokenExceptionWhenTokenIsExpired() {
        JwtUtil alreadyExpiredIssuer = new JwtUtil(SECRET, -1_000);

        String token = alreadyExpiredIssuer.generateToken(UUID.randomUUID());

        assertThatThrownBy(() -> jwtUtil.parseClaims(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parseClaims_throwsInvalidTokenExceptionWhenSignedWithADifferentKey() {
        String otherSecret = "b25lLXR3by10aHJlZS1mb3VyLWZpdmUtc2l4LXNldmVuLWVpZ2h0";
        JwtUtil differentIssuer = new JwtUtil(otherSecret, EXPIRATION_MS);
        String token = differentIssuer.generateToken(UUID.randomUUID());

        assertThatThrownBy(() -> jwtUtil.parseClaims(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parseClaims_throwsInvalidTokenExceptionWhenTokenIsTampered() {
        String token = jwtUtil.generateToken(UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> jwtUtil.parseClaims(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parseClaims_throwsInvalidTokenExceptionWhenTokenIsNotAJwtAtAll() {
        assertThatThrownBy(() -> jwtUtil.parseClaims("not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
