package com.enums;

public class Enums {
    // This class can be used to group related enums if needed
    public enum ApiKeyStatus {
        ACTIVE,
        INACTIVE
    }

    public enum Tier {
        FREE,
        PREMIUM
    }
    
    public enum PriorityType {
        HIGH_PRIORITY,
        DEFAULT_PRIORITY
    }

    public enum TaskType {
        SIMULATED_WORK,
        SEND_EMAIL
    }

    public enum TaskStatus {
        SCHEDULED,   // scheduled_at is in the future, sitting in Redis sorted set
        QUEUED,      // on the broker (SQS/Redis Stream), ready for a worker to claim
        RUNNING,     // a worker has claimed it and is actively executing
        SUCCESS,     // terminal — completed without error
        FAILED,      // execution threw an exception, eligible for retry evaluation
        RETRYING,    // backoff period in progress, waiting to re-enter QUEUED
        DEAD,        // terminal — retry_count reached max_retries, moved to DLQ
        CANCELLED    // terminal — admin-initiated cancellation
    }

    public enum TaskExecutionStatus {
        RUNNING,
        SUCCESS,
        FAILED
    }

    public enum ErrorType {
        TRANSIENT,
        PERMANENT
    }

    public enum UserRole {
        ADMIN,
        USER
    }

    public enum PlanTier {
        FREE,
        PREMIUM
    }

    public enum QueueType {
        HIGH_PRIORITY,
        DEFAULT_PRIORITY,
        DEAD_LETTER_QUEUE
    }

    public enum TenantStatus {
        ACTIVE,
        INACTIVE
    }
}