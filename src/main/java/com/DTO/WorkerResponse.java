package com.DTO;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.entity.Worker;
import com.enums.Enums.QueueType;
import com.enums.Enums.WorkerStatus;

@Getter
@AllArgsConstructor
public class WorkerResponse {
    private UUID id;
    private String name;
    private QueueType queueType;
    private WorkerStatus status;
    private Instant lastHeartbeat;
    private Instant startedAt;
    private String hostname;
    private int pid;
    private int capacity;
    private String version;

    public static WorkerResponse from(Worker worker) {
        return new WorkerResponse(
            worker.getId(), worker.getName(), worker.getQueueType(), worker.getStatus(),
            worker.getLastHeartbeat(), worker.getStartedAt(), worker.getHostname(),
            worker.getPid(), worker.getCapacity(), worker.getVersion()
        );
    }
}
