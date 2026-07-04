package com.exception;

import java.util.UUID;

public class TaskExecutionNotFoundException extends RuntimeException {
    public TaskExecutionNotFoundException(UUID taskId) {
        super("Task execution not found for task: " + taskId);
    }
}
