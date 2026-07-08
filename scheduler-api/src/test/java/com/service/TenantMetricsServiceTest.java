package com.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantMetricsServiceTest {

    @Mock
    private TenantRepo tenantRepo;
    @Mock
    private TenantMetricsRepo tenantMetricsRepo;
    @Mock
    private TaskRepo taskRepo;
    @Mock
    private TaskExecutionRepo taskExecutionRepo;
    @Mock
    private PlanRepo planRepo;

    private TenantMetricsService tenantMetricsService;

    @BeforeEach
    void setUp() {
        tenantMetricsService = new TenantMetricsService(tenantRepo, tenantMetricsRepo, taskRepo, taskExecutionRepo, planRepo);
    }

    private Task taskWithStatus(UUID tenantId, TaskStatus status) {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTenantId(tenantId);
        task.setTaskStatus(status);
        return task;
    }

    @Test
    void refreshTenantMetrics_countsTasksByStatusAndComputesRates() {
        UUID tenantId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setPlanId(planId);

        List<Task> tasks = List.of(
                taskWithStatus(tenantId, TaskStatus.QUEUED),
                taskWithStatus(tenantId, TaskStatus.RUNNING),
                taskWithStatus(tenantId, TaskStatus.FAILED),
                taskWithStatus(tenantId, TaskStatus.RETRYING),
                taskWithStatus(tenantId, TaskStatus.DEAD),
                taskWithStatus(tenantId, TaskStatus.SUCCESS),
                taskWithStatus(tenantId, TaskStatus.CANCELLED));
        tasks.get(4).setMovedToDlqAt(Instant.now()); // the DEAD task moved to DLQ

        Plan plan = new Plan();
        plan.setRateLimit(42);

        when(taskRepo.findByTenantId(tenantId)).thenReturn(tasks);
        when(taskExecutionRepo.findByTaskIdIn(anyList())).thenReturn(List.of());
        when(planRepo.findById(planId)).thenReturn(Optional.of(plan));
        when(tenantMetricsRepo.findById(tenantId)).thenReturn(Optional.empty());
        when(tenantMetricsRepo.save(any(TenantMetrics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantMetrics metrics = tenantMetricsService.refreshTenantMetrics(tenant);

        assertThat(metrics.getTenantId()).isEqualTo(tenantId);
        assertThat(metrics.getTotalTasks()).isEqualTo(7);
        assertThat(metrics.getQueuedTasks()).isEqualTo(1);
        assertThat(metrics.getRunningTasks()).isEqualTo(1);
        assertThat(metrics.getFailedTasks()).isEqualTo(1);
        assertThat(metrics.getRetryingTasks()).isEqualTo(1);
        assertThat(metrics.getDeadTasks()).isEqualTo(1);
        assertThat(metrics.getSuccessTasks()).isEqualTo(1);
        assertThat(metrics.getCancelledTasks()).isEqualTo(1);
        assertThat(metrics.getSuccessRate()).isEqualTo(1.0 / 7);
        assertThat(metrics.getFailureRate()).isEqualTo(1.0 / 7);
        assertThat(metrics.getTasksMovedToDlq()).isEqualTo(1);
        assertThat(metrics.getDlqRate()).isEqualTo(1.0 / 7);
        assertThat(metrics.getCurrentRateLimit()).isEqualTo(42);
        assertThat(metrics.getComputedAt()).isNotNull();
    }

    @Test
    void refreshTenantMetrics_zerosRatesInsteadOfDividingByZeroWhenTenantHasNoTasks() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setPlanId(UUID.randomUUID());

        when(taskRepo.findByTenantId(tenantId)).thenReturn(List.of());
        when(planRepo.findById(any())).thenReturn(Optional.empty());
        when(tenantMetricsRepo.findById(tenantId)).thenReturn(Optional.empty());
        when(tenantMetricsRepo.save(any(TenantMetrics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantMetrics metrics = tenantMetricsService.refreshTenantMetrics(tenant);

        assertThat(metrics.getTotalTasks()).isZero();
        assertThat(metrics.getSuccessRate()).isZero();
        assertThat(metrics.getFailureRate()).isZero();
        assertThat(metrics.getDlqRate()).isZero();
        assertThat(metrics.getAvgExecutionMs()).isZero();
        assertThat(metrics.getAvgQueueWaitMs()).isZero();
        assertThat(metrics.getCurrentRateLimit()).isZero();
        // no tasks means no task ids to look executions up for
        verify(taskExecutionRepo, never()).findByTaskIdIn(any());
    }

    @Test
    void refreshTenantMetrics_computesAverageExecutionAndQueueWaitTimes() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setPlanId(UUID.randomUUID());

        Instant scheduledAt = Instant.parse("2026-01-01T00:00:00Z");
        Task task1 = taskWithStatus(tenantId, TaskStatus.SUCCESS);
        task1.setScheduledAt(scheduledAt);
        Task task2 = taskWithStatus(tenantId, TaskStatus.SUCCESS);
        task2.setScheduledAt(scheduledAt);

        TaskExecution execution1 = new TaskExecution();
        execution1.setTaskId(task1.getId());
        execution1.setStartedAt(scheduledAt.plusMillis(100));
        execution1.setFinishedAt(scheduledAt.plusMillis(300)); // 200ms duration, 100ms queue wait

        TaskExecution execution2 = new TaskExecution();
        execution2.setTaskId(task2.getId());
        execution2.setStartedAt(scheduledAt.plusMillis(50));
        execution2.setFinishedAt(scheduledAt.plusMillis(150)); // 100ms duration, 50ms queue wait

        when(taskRepo.findByTenantId(tenantId)).thenReturn(List.of(task1, task2));
        when(taskExecutionRepo.findByTaskIdIn(anyList())).thenReturn(List.of(execution1, execution2));
        when(planRepo.findById(any())).thenReturn(Optional.empty());
        when(tenantMetricsRepo.findById(tenantId)).thenReturn(Optional.empty());
        when(tenantMetricsRepo.save(any(TenantMetrics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantMetrics metrics = tenantMetricsService.refreshTenantMetrics(tenant);

        assertThat(metrics.getAvgExecutionMs()).isEqualTo(150.0);
        assertThat(metrics.getAvgQueueWaitMs()).isEqualTo(75.0);
    }

    @Test
    void refreshTenantMetrics_reusesExistingMetricsRowInsteadOfCreatingANewOne() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setPlanId(UUID.randomUUID());

        TenantMetrics existing = new TenantMetrics();
        existing.setTenantId(tenantId);
        existing.setTotalTasks(999); // stale value that should be overwritten, not appended to

        when(taskRepo.findByTenantId(tenantId)).thenReturn(List.of(taskWithStatus(tenantId, TaskStatus.QUEUED)));
        when(taskExecutionRepo.findByTaskIdIn(anyList())).thenReturn(List.of());
        when(planRepo.findById(any())).thenReturn(Optional.empty());
        when(tenantMetricsRepo.findById(tenantId)).thenReturn(Optional.of(existing));
        when(tenantMetricsRepo.save(any(TenantMetrics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantMetrics result = tenantMetricsService.refreshTenantMetrics(tenant);

        assertThat(result).isSameAs(existing);
        assertThat(result.getTotalTasks()).isEqualTo(1);
    }

    @Test
    void refreshAllTenantMetrics_refreshesEveryTenant() {
        Tenant tenant1 = new Tenant();
        tenant1.setId(UUID.randomUUID());
        tenant1.setPlanId(UUID.randomUUID());
        Tenant tenant2 = new Tenant();
        tenant2.setId(UUID.randomUUID());
        tenant2.setPlanId(UUID.randomUUID());

        when(tenantRepo.findAll()).thenReturn(List.of(tenant1, tenant2));
        when(taskRepo.findByTenantId(any())).thenReturn(List.of());
        when(planRepo.findById(any())).thenReturn(Optional.empty());
        when(tenantMetricsRepo.findById(any())).thenReturn(Optional.empty());
        when(tenantMetricsRepo.save(any(TenantMetrics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tenantMetricsService.refreshAllTenantMetrics();

        verify(taskRepo).findByTenantId(tenant1.getId());
        verify(taskRepo).findByTenantId(tenant2.getId());
        verify(tenantMetricsRepo, times(2)).save(any(TenantMetrics.class));
    }
}
