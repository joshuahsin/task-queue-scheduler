package com.DTO;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.entity.TenantMetrics;

@Getter
@AllArgsConstructor
public class TenantMetricsResponse {
    private UUID tenantId;
    private long totalTasks;
    private long queuedTasks;
    private long runningTasks;
    private long scheduledTasks;
    private long failedTasks;
    private long retryingTasks;
    private long deadTasks;
    private long successTasks;
    private long cancelledTasks;
    private double avgExecutionMs;
    private double avgQueueWaitMs;
    private double successRate;
    private double failureRate;
    private long rateLimitHits;
    private int currentRateLimit;
    private long tasksMovedToDlq;
    private double dlqRate;
    private Instant computedAt;

    public static TenantMetricsResponse from(TenantMetrics metrics) {
        return new TenantMetricsResponse(
            metrics.getTenantId(), metrics.getTotalTasks(), metrics.getQueuedTasks(),
            metrics.getRunningTasks(), metrics.getScheduledTasks(), metrics.getFailedTasks(),
            metrics.getRetryingTasks(), metrics.getDeadTasks(), metrics.getSuccessTasks(),
            metrics.getCancelledTasks(), metrics.getAvgExecutionMs(), metrics.getAvgQueueWaitMs(),
            metrics.getSuccessRate(), metrics.getFailureRate(), metrics.getRateLimitHits(),
            metrics.getCurrentRateLimit(), metrics.getTasksMovedToDlq(), metrics.getDlqRate(),
            metrics.getComputedAt()
        );
    }
}
