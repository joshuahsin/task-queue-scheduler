package com.DAO;

import java.security.Timestamp;
import java.util.UUID;

import com.enums.Enums.TaskType;
import com.enums.Enums.PriorityType;

public interface TasksDAO {
    public UUID queueTask(TaskType type, PriorityType priority, Timestamp scheduledAt);
}
