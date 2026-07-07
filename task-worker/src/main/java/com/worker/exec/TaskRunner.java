package com.worker.exec;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TaskRunner {
    private static final Logger log = LoggerFactory.getLogger(TaskRunner.class);

    public boolean simulateWork() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(200, 800));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ThreadLocalRandom.current().nextInt(100) >= 20; // ~80% simulated success rate
    }

    public boolean simulateSendEmail(Map<String, Object> payload) {
        log.info("Simulated sending email, payload={}", payload);
        return true;
    }
}
