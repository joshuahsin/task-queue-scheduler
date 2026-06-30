package com.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

enum Status {
    RUNNING,
    SUCCESS,
    FAILED
}

enum ErrorType {
    TRANSIENT,
    PERMANENT
}

@Entity
@Table(name = "task_executions")
class TaskExecution {
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    private UUID workerId;
    private int attemptNumber;
    private Instant startedAt;
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private ErrorType errorType;
    private String errorMessage;
}