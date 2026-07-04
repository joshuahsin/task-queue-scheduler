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

import com.enums.Enums.QueueType;
import com.enums.Enums.WorkerStatus;

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
    private QueueType queueType;  // HIGH_PRIORITY, DEFAULT_PRIORITY, DEAD_LETTER_QUEUE

    @Enumerated(EnumType.STRING)
    private WorkerStatus status;  // ONLINE, OFFLINE, UNHEALTHY
    private Instant lastHeartbeat;
    private Instant startedAt;

    private String hostname;
    private int pid;
    private int capacity;         // max concurrent tasks this worker's thread pool can run
    private String version;       // worker build/version string
}
