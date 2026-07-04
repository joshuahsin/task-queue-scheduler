package com.exception;

import java.util.UUID;

public class WorkerNotFoundException extends RuntimeException {
    public WorkerNotFoundException(UUID workerId) {
        super("Worker not found: " + workerId);
    }
}
