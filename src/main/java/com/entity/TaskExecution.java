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
import com.enums.Enums.TaskExecutionStatus;
import com.enums.Enums.ErrorType;

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
    private TaskExecutionStatus status;

    @Enumerated(EnumType.STRING)
    private ErrorType errorType;
    private String errorMessage;
}