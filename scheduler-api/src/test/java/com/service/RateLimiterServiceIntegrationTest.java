package com.service;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

// Real-Redis integration test for RateLimiterService, backed by Testcontainers rather than a
// manually-started local/remote Redis — self-contained, no secrets, no external network
// dependency, portable to any machine/CI with Docker. Requires Docker to run; this environment
// doesn't have it available, same constraint that already applies to the existing Postgres-backed
// TaskQueueSchedulerApplicationTests context-load test.
@Testcontainers
class RateLimiterServiceIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static RateLimiterService rateLimiterService;

    @BeforeAll
    static void startRedisClient() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redis.getHost(), redis.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        rateLimiterService = new RateLimiterService(redisTemplate, 2); // 2-second window, fast test
    }

    @AfterAll
    static void closeRedisClient() {
        connectionFactory.destroy();
    }

    @Test
    void tryAcquire_allowsUpToLimitThenRejectsThenResetsAfterWindow() throws InterruptedException {
        UUID tenantId = UUID.randomUUID();
        int limit = 3;

        for (int i = 1; i <= limit; i++) {
            assertThat(rateLimiterService.tryAcquire(tenantId, limit)).as("request %d within limit", i).isTrue();
        }

        assertThat(rateLimiterService.tryAcquire(tenantId, limit)).as("request over limit").isFalse();

        Thread.sleep(2500); // past the 2-second window

        assertThat(rateLimiterService.tryAcquire(tenantId, limit)).as("request after window reset").isTrue();
    }
}
