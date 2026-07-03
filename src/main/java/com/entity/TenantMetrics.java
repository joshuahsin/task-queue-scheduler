package com.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenant_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantMetrics {
    @Id
    UUID tenantId; // same value as the owning Tenant's id — this is a keyed snapshot, not an independent row

    // task counts
    long totalTasks;
    long queuedTasks;
    long runningTasks;
    long scheduledTasks;
    long failedTasks;
    long retryingTasks;
    long deadTasks;
    long successTasks;
    long cancelledTasks;

    // performance
    double avgExecutionMs;
    double avgQueueWaitMs;
    double successRate;
    double failureRate;

    // rate limiting
    long rateLimitHits;
    int currentRateLimit;

    // DLQ
    long tasksMovedToDlq;
    double dlqRate;

    // metadata
    Instant computedAt;    // when this snapshot was calculated — useful since it's cached
}
