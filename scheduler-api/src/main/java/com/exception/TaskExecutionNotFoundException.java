package com.exception;

import java.util.UUID;

public class TaskExecutionNotFoundException extends RuntimeException {
    public TaskExecutionNotFoundException(UUID id) {
        super("Task execution not found: " + id);
    }
}
