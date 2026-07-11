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
        // Flip a character in the middle of the payload segment, not the token's last character.
        // Base64's final character in a run can have unused padding bits, so swapping it for a
        // fixed alternate character isn't guaranteed to change the decoded bytes — flakily passing
        // or failing depending on which character the random UUID happened to produce there. An
        // interior character always decodes all 6 of its bits meaningfully, so flipping it reliably
        // changes the payload's actual bytes, which reliably invalidates the signature.
        String[] parts = token.split("\\.");
        String payload = parts[1];
        int middle = payload.length() / 2;
        char flipped = payload.charAt(middle) == 'A' ? 'B' : 'A';
        String tamperedPayload = payload.substring(0, middle) + flipped + payload.substring(middle + 1);
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThatThrownBy(() -> jwtUtil.parseClaims(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parseClaims_throwsInvalidTokenExceptionWhenTokenIsNotAJwtAtAll() {
        assertThatThrownBy(() -> jwtUtil.parseClaims("not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
