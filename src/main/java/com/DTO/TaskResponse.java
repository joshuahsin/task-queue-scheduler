package com.DTO;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.entity.Task;
import com.enums.Enums.PriorityType;
import com.enums.Enums.TaskStatus;
import com.enums.Enums.TaskType;

@Getter
@AllArgsConstructor
public class TaskResponse {
    private UUID id;
    private UUID tenantId;
    private TaskType taskType;
    private PriorityType priority;
    private TaskStatus taskStatus;
    private Map<String, Object> payload;
    private Instant scheduledAt;
    private int retryCount;
    private Instant createdAt;

    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(), task.getTenantId(), task.getTaskType(), task.getPriority(),
            task.getTaskStatus(), task.getPayload(), task.getScheduledAt(),
            task.getRetryCount(), task.getCreatedAt()
        );
    }
}
