package com.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

enum QueueType {
    HIGH,
    DEFAULT,
    DLQ
}

enum WorkerStatus {
    ONLINE,
    OFFLINE,
    UNHEALTHY
}

@Entity
@Table(name = "workers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;          // e.g. "worker-high-1"

    @Enumerated(EnumType.STRING)
    private QueueType queueType;  // HIGH, DEFAULT, DLQ (note: not "SCHEDULED" as priority — separate concept)

    @Enumerated(EnumType.STRING)
    private WorkerStatus status;  // ONLINE, OFFLINE, UNHEALTHY
    private Instant lastHeartbeat;
    private Instant startedAt;
}
