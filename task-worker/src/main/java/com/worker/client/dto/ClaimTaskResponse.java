package com.worker.client.dto;

import java.util.Map;
import java.util.UUID;

import com.worker.model.ClaimOutcome;
import com.worker.model.TaskType;

public record ClaimTaskResponse(ClaimOutcome outcome, UUID executionId, Integer attemptNumber,
        TaskType taskType, Map<String, Object> payload, String skipReason) {
}
