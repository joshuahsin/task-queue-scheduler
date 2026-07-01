package com.DAO;

import java.util.List;
import java.util.UUID;

import com.entity.TaskExecution;

public interface TaskExecutionDAO {
    public List<TaskExecution> getTaskExecutions(UUID taskId);
    public boolean CancelTaskExecution(UUID taskId);
}