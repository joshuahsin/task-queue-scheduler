package com.DAO;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.enums.Enums.TaskType;
import com.enums.Enums.PriorityType;
import com.enums.Enums.TaskStatus;

import com.entity.Task;

public interface TaskDAO {
    public UUID queueTask(UUID tenantId, TaskType type, PriorityType priority, Instant scheduledAt);
    public boolean cancelTask(UUID taskId, UUID tenantId);
    public Optional<Task> getTask(UUID taskId, UUID tenantId);
    public Page<Task> getTasksQuery(UUID tenantId, int priority, TaskStatus status, int page, int size);
}
