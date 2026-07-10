package com.worker.exec;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskRunnerTest {

    private final TaskRunner taskRunner = new TaskRunner();

    // simulateWork()'s ~80% success rate comes from ThreadLocalRandom, which can't be seeded
    // deterministically from outside — so this doesn't assert the exact distribution (that would
    // either be flaky or require sleeping through dozens of trials to be statistically safe).
    // What's verifiable and meaningful instead: it actually sleeps for roughly the documented
    // 200-800ms range rather than, say, returning instantly or hanging.
    @Test
    void simulateWork_sleepsWithinExpectedRange() {
        long start = System.nanoTime();
        taskRunner.simulateWork();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isBetween(150L, 2000L);
    }

    @Test
    void simulateSendEmail_alwaysReturnsTrue() {
        boolean result = taskRunner.simulateSendEmail(Map.of("to", "a@b.com"));

        assertThat(result).isTrue();
    }

    @Test
    void simulateSendEmail_handlesNullPayloadWithoutThrowing() {
        boolean result = taskRunner.simulateSendEmail(null);

        assertThat(result).isTrue();
    }
}
