package com.service;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(redisTemplate, 60);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void tryAcquire_allowsWhenUnderLimit() {
        when(valueOperations.increment(anyString())).thenReturn(5L);

        boolean allowed = rateLimiterService.tryAcquire(UUID.randomUUID(), 60);

        assertThat(allowed).isTrue();
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    void tryAcquire_rejectsWhenOverLimit() {
        when(valueOperations.increment(anyString())).thenReturn(61L);

        boolean allowed = rateLimiterService.tryAcquire(UUID.randomUUID(), 60);

        assertThat(allowed).isFalse();
    }

    @Test
    void tryAcquire_allowsWhenExactlyAtLimit() {
        when(valueOperations.increment(anyString())).thenReturn(60L);

        assertThat(rateLimiterService.tryAcquire(UUID.randomUUID(), 60)).isTrue();
    }

    @Test
    void tryAcquire_failsOpenWhenRedisThrows() {
        when(valueOperations.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        boolean allowed = rateLimiterService.tryAcquire(UUID.randomUUID(), 60);

        assertThat(allowed).isTrue();
    }

    @Test
    void tryAcquire_keyIncludesTenantId() {
        UUID tenantId = UUID.randomUUID();
        when(valueOperations.increment(anyString())).thenReturn(1L);

        rateLimiterService.tryAcquire(tenantId, 60);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).increment(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).contains(tenantId.toString());
    }
}
