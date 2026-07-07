package com.DTO;

import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.entity.Task;
import com.entity.TaskExecution;
import com.enums.Enums.ClaimOutcome;
import com.enums.Enums.TaskType;

@Getter
@AllArgsConstructor
public class ClaimTaskResponse {
    private ClaimOutcome outcome;
    private UUID executionId;
    private Integer attemptNumber;
    private TaskType taskType;
    private Map<String, Object> payload;
    private String skipReason;

    public static ClaimTaskResponse claimed(TaskExecution execution, Task task) {
        return new ClaimTaskResponse(ClaimOutcome.CLAIMED, execution.getId(), execution.getAttemptNumber(),
                task.getTaskType(), task.getPayload(), null);
    }

    public static ClaimTaskResponse skipped(String reason) {
        return new ClaimTaskResponse(ClaimOutcome.SKIPPED, null, null, null, null, reason);
    }
}
