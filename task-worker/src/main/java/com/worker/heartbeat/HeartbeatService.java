package com.worker.heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.worker.client.SchedulerApiClient;
import com.worker.registration.WorkerRegistrationHolder;

@Service
public class HeartbeatService {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatService.class);

    private final SchedulerApiClient schedulerApiClient;
    private final WorkerRegistrationHolder registrationHolder;

    public HeartbeatService(SchedulerApiClient schedulerApiClient, WorkerRegistrationHolder registrationHolder) {
        this.schedulerApiClient = schedulerApiClient;
        this.registrationHolder = registrationHolder;
    }

    @Scheduled(fixedDelayString = "${worker.heartbeat-interval-ms:30000}")
    public void heartbeat() {
        try {
            schedulerApiClient.heartbeat(registrationHolder.requireWorkerId());
        } catch (Exception e) {
            log.warn("Heartbeat to scheduler API failed", e);
        }
    }
}
