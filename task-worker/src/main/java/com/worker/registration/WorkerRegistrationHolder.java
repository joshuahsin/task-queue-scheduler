package com.worker.registration;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class WorkerRegistrationHolder {
    private volatile UUID workerId;

    public void setWorkerId(UUID workerId) {
        this.workerId = workerId;
    }

    public UUID requireWorkerId() {
        if (workerId == null) {
            throw new IllegalStateException("Worker has not registered with the scheduler API yet");
        }
        return workerId;
    }
}
