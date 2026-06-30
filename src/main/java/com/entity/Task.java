package com.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToOne;

enum PriorityType {
    HIGH_PRIORITY,
    DEFAULT_PRIORITY
}

enum TaskType {
    SIMULATED_WORK,
    SEND_EMAIL
}

enum TaskStatus {
    SCHEDULED,   // scheduled_at is in the future, sitting in Redis sorted set
    QUEUED,      // on the broker (SQS/Redis Stream), ready for a worker to claim
    RUNNING,     // a worker has claimed it and is actively executing
    SUCCESS,     // terminal — completed without error
    FAILED,      // execution threw an exception, eligible for retry evaluation
    RETRYING,    // backoff period in progress, waiting to re-enter QUEUED
    DEAD,        // terminal — retry_count reached max_retries, moved to DLQ
    CANCELLED    // terminal — admin-initiated cancellation
}

public class Task {
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    private PriorityType priority;

    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    private TaskStatus taskStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    private String payloadRef;

    private Instant scheduledAt;
    private int retryCount;
    private Instant createdAt;
    private Instant movedToDlqAt;
}
