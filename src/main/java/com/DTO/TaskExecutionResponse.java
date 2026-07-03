package com.DTO;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.entity.TaskExecution;
import com.enums.Enums.ErrorType;
import com.enums.Enums.TaskExecutionStatus;

@Getter
@AllArgsConstructor
public class TaskExecutionResponse {
    private UUID id;
    private UUID taskId;
    private UUID workerId;
    private int attemptNumber;
    private Instant startedAt;
    private Instant finishedAt;
    private TaskExecutionStatus status;
    private ErrorType errorType;
    private String errorMessage;

    public static TaskExecutionResponse from(TaskExecution execution) {
        return new TaskExecutionResponse(
            execution.getId(), execution.getTaskId(), execution.getWorkerId(),
            execution.getAttemptNumber(), execution.getStartedAt(), execution.getFinishedAt(),
            execution.getStatus(), execution.getErrorType(), execution.getErrorMessage()
        );
    }
}
