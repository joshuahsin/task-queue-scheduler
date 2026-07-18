package com.worker.registration;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.worker.client.SchedulerApiClient;

// Symmetric counterpart to WorkerBootstrap: that one registers this process at startup, this one
// deregisters it on a graceful shutdown (marks it OFFLINE immediately in scheduler-api instead of
// waiting for the passive health sweep's timeout). Only fires reliably if the JVM actually receives
// SIGTERM directly — see the Dockerfile's exec-form ENTRYPOINT.
@Component
public class WorkerShutdownHandler {
    private static final Logger log = LoggerFactory.getLogger(WorkerShutdownHandler.class);

    private final SchedulerApiClient schedulerApiClient;
    private final WorkerRegistrationHolder registrationHolder;

    public WorkerShutdownHandler(SchedulerApiClient schedulerApiClient, WorkerRegistrationHolder registrationHolder) {
        this.schedulerApiClient = schedulerApiClient;
        this.registrationHolder = registrationHolder;
    }

    @PreDestroy
    public void deregister() {
        try {
            schedulerApiClient.deregister(registrationHolder.requireWorkerId());
        } catch (Exception e) {
            // Never let a slow/failed deregister call block shutdown — this also covers the case
            // where the process never finished registering in the first place (requireWorkerId()
            // throws IllegalStateException), so a fast-failed startup doesn't spam an error here too.
            log.warn("Failed to deregister from scheduler API during shutdown", e);
        }
    }
}
