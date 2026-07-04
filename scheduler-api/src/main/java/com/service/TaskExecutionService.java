package com.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.DAO.TaskExecutionDAO;
import com.entity.TaskExecution;
import com.enums.Enums.TaskExecutionStatus;
import com.repo.TaskExecutionRepo;

@Service
public class TaskExecutionService implements TaskExecutionDAO {
    private final TaskExecutionRepo taskExecutionRepo;

    public TaskExecutionService(TaskExecutionRepo taskExecutionRepo) {
        this.taskExecutionRepo = taskExecutionRepo;
    }

    @Override
    public List<TaskExecution> getTaskExecutions(UUID taskId) {
        return taskExecutionRepo.findByTaskId(taskId);
    }

    @Override
    public boolean cancelTaskExecution(UUID taskId) {
        return taskExecutionRepo.findById(taskId)
            .map(execution -> {
                execution.setStatus(TaskExecutionStatus.CANCELLED);
                taskExecutionRepo.save(execution);
                return true;
            })
            .orElse(false);
    }
}
