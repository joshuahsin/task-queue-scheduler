package com.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.DAO.WorkerTaskDAO;
import com.DTO.ClaimTaskResponse;
import com.entity.Plan;
import com.entity.Task;
import com.entity.TaskExecution;
import com.enums.Enums.ErrorType;
import com.enums.Enums.TaskExecutionStatus;
import com.enums.Enums.TaskStatus;
import com.exception.TaskExecutionNotFoundException;
import com.exception.WorkerNotFoundException;
import com.repo.PlanRepo;
import com.repo.TaskExecutionRepo;
import com.repo.TaskRepo;
import com.repo.TenantRepo;
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
    private final TenantRepo tenantRepo;
    private final PlanRepo planRepo;

    public WorkerTaskService(TaskRepo taskRepo, TaskExecutionRepo taskExecutionRepo, WorkerRepo workerRepo,
            TenantRepo tenantRepo, PlanRepo planRepo) {
        this.taskRepo = taskRepo;
        this.taskExecutionRepo = taskExecutionRepo;
        this.workerRepo = workerRepo;
        this.tenantRepo = tenantRepo;
        this.planRepo = planRepo;
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

            if (task.getRetryCount() >= resolveMaxRetries(task.getTenantId())) {
                // Retries exhausted — stop asking workers to reclaim this task. OCI Queue's own
                // max-delivery-attempts setting independently governs when the *message* itself
                // gets moved to its infrastructure-level DLQ; this is the application-level
                // decision to stop retrying, tracked here so it shows up in TenantMetrics.
                task.setTaskStatus(TaskStatus.DEAD);
                task.setMovedToDlqAt(Instant.now());
            } else {
                task.setTaskStatus(TaskStatus.RETRYING);
            }
        }

        taskExecutionRepo.save(execution);
        taskRepo.save(task);
    }

    // A Tenant's maxRetriesOverride of 0 means "no override" (the default for every tenant unless
    // explicitly set), not "zero retries allowed" — falls back to the tenant's Plan.maxRetries in
    // that case. When an override IS set, it can only ever reduce the effective limit below the
    // plan's ceiling, never raise it above what the tenant's plan actually entitles them to — the
    // override exists for tenants to opt into fewer retries, not to bypass their plan's tier.
    // Resolves to Integer.MAX_VALUE (effectively "never mark DEAD due to retry count") if the
    // tenant or plan can't be resolved at all, rather than inventing a new failure mode for a case
    // that shouldn't normally happen — the same "let it proceed" bias already used for an
    // unresolvable tenant/plan in TaskService.enforceRateLimit.
    private int resolveMaxRetries(UUID tenantId) {
        return tenantRepo.findById(tenantId)
                .map(tenant -> {
                    int planMaxRetries = planRepo.findById(tenant.getPlanId())
                            .map(Plan::getMaxRetries)
                            .orElse(Integer.MAX_VALUE);
                    return tenant.getMaxRetriesOverride() > 0
                            ? Math.min(tenant.getMaxRetriesOverride(), planMaxRetries)
                            : planMaxRetries;
                })
                .orElse(Integer.MAX_VALUE);
    }
}
