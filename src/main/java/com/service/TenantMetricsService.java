package com.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.entity.Plan;
import com.entity.Task;
import com.entity.TaskExecution;
import com.entity.Tenant;
import com.entity.TenantMetrics;
import com.enums.Enums.TaskStatus;
import com.repo.PlanRepo;
import com.repo.TaskExecutionRepo;
import com.repo.TaskRepo;
import com.repo.TenantMetricsRepo;
import com.repo.TenantRepo;

@Service
public class TenantMetricsService {
    private final TenantRepo tenantRepo;
    private final TenantMetricsRepo tenantMetricsRepo;
    private final TaskRepo taskRepo;
    private final TaskExecutionRepo taskExecutionRepo;
    private final PlanRepo planRepo;

    public TenantMetricsService(TenantRepo tenantRepo, TenantMetricsRepo tenantMetricsRepo,
            TaskRepo taskRepo, TaskExecutionRepo taskExecutionRepo, PlanRepo planRepo) {
        this.tenantRepo = tenantRepo;
        this.tenantMetricsRepo = tenantMetricsRepo;
        this.taskRepo = taskRepo;
        this.taskExecutionRepo = taskExecutionRepo;
        this.planRepo = planRepo;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void refreshAllTenantMetrics() {
        tenantRepo.findAll().forEach(this::refreshTenantMetrics);
    }

    public TenantMetrics refreshTenantMetrics(Tenant tenant) {
        UUID tenantId = tenant.getId();
        List<Task> tasks = taskRepo.findByTenantId(tenantId);
        long totalTasks = tasks.size();

        Map<TaskStatus, Long> countsByStatus = tasks.stream()
            .collect(Collectors.groupingBy(Task::getTaskStatus, Collectors.counting()));

        long queuedTasks = countsByStatus.getOrDefault(TaskStatus.QUEUED, 0L);
        long runningTasks = countsByStatus.getOrDefault(TaskStatus.RUNNING, 0L);
        long failedTasks = countsByStatus.getOrDefault(TaskStatus.FAILED, 0L);
        long retryingTasks = countsByStatus.getOrDefault(TaskStatus.RETRYING, 0L);
        long deadTasks = countsByStatus.getOrDefault(TaskStatus.DEAD, 0L);
        long successTasks = countsByStatus.getOrDefault(TaskStatus.SUCCESS, 0L);
        long cancelledTasks = countsByStatus.getOrDefault(TaskStatus.CANCELLED, 0L);

        Map<UUID, Task> taskById = tasks.stream().collect(Collectors.toMap(Task::getId, t -> t));
        List<TaskExecution> executions = taskById.isEmpty()
            ? List.of()
            : taskExecutionRepo.findByTaskIdIn(List.copyOf(taskById.keySet()));

        double avgExecutionMs = executions.stream()
            .filter(e -> e.getStartedAt() != null && e.getFinishedAt() != null)
            .mapToLong(e -> Duration.between(e.getStartedAt(), e.getFinishedAt()).toMillis())
            .average()
            .orElse(0);

        double avgQueueWaitMs = executions.stream()
            .filter(e -> e.getStartedAt() != null
                && taskById.get(e.getTaskId()) != null
                && taskById.get(e.getTaskId()).getScheduledAt() != null)
            .mapToLong(e -> Duration.between(taskById.get(e.getTaskId()).getScheduledAt(), e.getStartedAt()).toMillis())
            .average()
            .orElse(0);

        double successRate = totalTasks == 0 ? 0 : (double) successTasks / totalTasks;
        double failureRate = totalTasks == 0 ? 0 : (double) failedTasks / totalTasks;

        long tasksMovedToDlq = tasks.stream().filter(t -> t.getMovedToDlqAt() != null).count();
        double dlqRate = totalTasks == 0 ? 0 : (double) tasksMovedToDlq / totalTasks;

        int currentRateLimit = planRepo.findById(tenant.getPlanId())
            .map(Plan::getRateLimit)
            .orElse(0);

        TenantMetrics metrics = tenantMetricsRepo.findById(tenantId).orElseGet(TenantMetrics::new);
        metrics.setTenantId(tenantId);
        metrics.setTotalTasks(totalTasks);
        metrics.setQueuedTasks(queuedTasks);
        metrics.setRunningTasks(runningTasks);
        metrics.setScheduledTasks(0); // no SCHEDULED task status exists anymore — always zero
        metrics.setFailedTasks(failedTasks);
        metrics.setRetryingTasks(retryingTasks);
        metrics.setDeadTasks(deadTasks);
        metrics.setSuccessTasks(successTasks);
        metrics.setCancelledTasks(cancelledTasks);
        metrics.setAvgExecutionMs(avgExecutionMs);
        metrics.setAvgQueueWaitMs(avgQueueWaitMs);
        metrics.setSuccessRate(successRate);
        metrics.setFailureRate(failureRate);
        // rateLimitHits is left untouched: no rate limiter is implemented yet to increment it.
        metrics.setCurrentRateLimit(currentRateLimit);
        metrics.setTasksMovedToDlq(tasksMovedToDlq);
        metrics.setDlqRate(dlqRate);
        metrics.setComputedAt(Instant.now());

        return tenantMetricsRepo.save(metrics);
    }
}
