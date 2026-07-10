package com.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

// Fixed-window counter per tenant, backed by Redis so counts are shared across API instances.
// Encoding the window boundary into the key itself (rather than a separate reset mechanism) means
// a new window is automatically a new key — no cleanup logic needed, Redis's own TTL expires it.
//
// expire() is called unconditionally after every increment() (not only when the count is 1) so
// that a crash between the two calls on one request still gets its TTL patched up by the very next
// request in the same window. This isn't fully atomic (a true fix would need a Lua script or
// MULTI/EXEC), but that would be the first scripting/transaction usage of Redis in this codebase —
// the residual risk here is one abandoned key for one already-past window, which is negligible.
//
// Trade-off accepted: a fixed window allows up to 2x burst right at a window boundary (e.g. the
// limit's worth of requests at the last second of one window, then again at the first second of
// the next). A sliding window would avoid that but needs sorted sets, which isn't used here.
@Service
public class RateLimiterService {
    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final StringRedisTemplate redisTemplate;
    private final int windowSeconds;

    public RateLimiterService(StringRedisTemplate redisTemplate,
            @Value("${rate-limit.window-seconds:60}") int windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.windowSeconds = windowSeconds;
    }

    public boolean tryAcquire(UUID tenantId, int limitPerWindow) {
        long windowStart = Instant.now().getEpochSecond() / windowSeconds;
        String key = "rate-limit:%s:%d".formatted(tenantId, windowStart);

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds + 10L));
            return count == null || count <= limitPerWindow;
        } catch (Exception e) {
            // Fail open: a rate limiter that's unreachable shouldn't block task creation entirely.
            log.warn("Redis unavailable while rate limiting tenant {} — failing open", tenantId, e);
            return true;
        }
    }
}
