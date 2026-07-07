package com.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.DAO.WorkerTaskDAO;
import com.DTO.ClaimTaskResponse;
import com.entity.Task;
import com.entity.TaskExecution;
import com.enums.Enums.ErrorType;
import com.enums.Enums.TaskExecutionStatus;
import com.enums.Enums.TaskStatus;
import com.exception.TaskExecutionNotFoundException;
import com.exception.WorkerNotFoundException;
import com.repo.TaskExecutionRepo;
import com.repo.TaskRepo;
import com.repo.WorkerRepo;

// Backs the worker-facing claim/complete endpoints on WorkerController. Kept separate from
// TaskService (which is tenant-scoped — a worker only has a bare task UUID off the queue, no
// tenant context) and from WorkerService/TaskExecutionService (neither touches all three of
// Task/TaskExecution/Worker together).
@Service
public class WorkerTaskService implements WorkerTaskDAO {
    private final TaskRepo taskRepo;
    private final TaskExecutionRepo taskExecutionRepo;
    private final WorkerRepo workerRepo;

    public WorkerTaskService(TaskRepo taskRepo, TaskExecutionRepo taskExecutionRepo, WorkerRepo workerRepo) {
        this.taskRepo = taskRepo;
        this.taskExecutionRepo = taskExecutionRepo;
        this.workerRepo = workerRepo;
    }

    @Override
    public ClaimTaskResponse claimTask(UUID workerId, UUID taskId) {
        if (!workerRepo.existsById(workerId)) {
            throw new WorkerNotFoundException(workerId);
        }

        Task task = taskRepo.findById(taskId).orElse(null);
        if (task == null) {
            return ClaimTaskResponse.skipped("TASK_NOT_FOUND");
        }

        TaskStatus status = task.getTaskStatus();
        if (status == TaskStatus.CANCELLED || status == TaskStatus.SUCCESS || status == TaskStatus.DEAD) {
            return ClaimTaskResponse.skipped("TASK_" + status.name());
        }

        task.setTaskStatus(TaskStatus.RUNNING);
        taskRepo.save(task);

        TaskExecution execution = new TaskExecution();
        execution.setTaskId(taskId);
        execution.setWorkerId(workerId);
        execution.setAttemptNumber(taskExecutionRepo.findByTaskId(taskId).size() + 1);
        execution.setStartedAt(Instant.now());
        execution.setStatus(TaskExecutionStatus.RUNNING);
        taskExecutionRepo.save(execution);

        return ClaimTaskResponse.claimed(execution, task);
    }

    @Override
    public void completeExecution(UUID workerId, UUID taskId, UUID executionId, boolean success, ErrorType errorType, String errorMessage) {
        TaskExecution execution = taskExecutionRepo.findById(executionId)
                .orElseThrow(() -> new TaskExecutionNotFoundException(executionId));
        if (!execution.getTaskId().equals(taskId)) {
            throw new IllegalArgumentException("Execution " + executionId + " does not belong to task " + taskId);
        }

        Task task = taskRepo.findById(taskId).orElseThrow(() -> new IllegalArgumentException("No such task: " + taskId));

        execution.setFinishedAt(Instant.now());
        if (success) {
            execution.setStatus(TaskExecutionStatus.SUCCESS);
            task.setTaskStatus(TaskStatus.SUCCESS);
        } else {
            execution.setStatus(TaskExecutionStatus.FAILED);
            execution.setErrorType(errorType);
            execution.setErrorMessage(errorMessage);
            task.setRetryCount(task.getRetryCount() + 1);
            task.setTaskStatus(TaskStatus.RETRYING);
        }

        taskExecutionRepo.save(execution);
        taskRepo.save(task);
    }
}
