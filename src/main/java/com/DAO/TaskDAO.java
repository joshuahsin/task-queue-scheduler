package com.DAO;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.enums.Enums.TaskType;
import com.enums.Enums.PriorityType;
import com.enums.Enums.TaskStatus;

import com.entity.Task;

public interface TaskDAO {
    public UUID queueTask(TaskType type, PriorityType priority, Instant scheduledAt);
    public boolean cancelTask(UUID taskId);
    public Task getTask(UUID taskId);
    public Page<Task> getTasksQuery(UUID tenantId, int priority, TaskStatus status, int retryCount, int page, int size);
}
