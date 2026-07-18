package com.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.DAO.WorkerDAO;
import com.entity.TaskExecution;
import com.entity.Worker;
import com.enums.Enums.QueueType;
import com.enums.Enums.WorkerStatus;
import com.repo.TaskExecutionRepo;
import com.repo.WorkerRepo;

@Service
public class WorkerService implements WorkerDAO {
    private final WorkerRepo workerRepo;
    private final TaskExecutionRepo taskExecutionRepo;
    private final long unhealthyAfterMs;
    private final long offlineAfterMs;

    public WorkerService(WorkerRepo workerRepo, TaskExecutionRepo taskExecutionRepo,
            @Value("${worker.health.unhealthy-after-ms:90000}") long unhealthyAfterMs,
            @Value("${worker.health.offline-after-ms:300000}") long offlineAfterMs) {
        this.workerRepo = workerRepo;
        this.taskExecutionRepo = taskExecutionRepo;
        this.unhealthyAfterMs = unhealthyAfterMs;
        this.offlineAfterMs = offlineAfterMs;
    }

    @Override
    public Worker registerWorker(String name, QueueType queueType, String hostname, int pid, int capacity, String version) {
        Worker worker = new Worker();
        worker.setName(name);
        worker.setQueueType(queueType);
        worker.setHostname(hostname);
        worker.setPid(pid);
        worker.setCapacity(capacity);
        worker.setVersion(version);
        worker.setStatus(WorkerStatus.ONLINE);

        Instant now = Instant.now();
        worker.setStartedAt(now);
        worker.setLastHeartbeat(now);

        return workerRepo.save(worker);
    }

    @Override
    public boolean heartbeatWorker(UUID workerId) {
        return workerRepo.findById(workerId)
            .map(worker -> {
                worker.setLastHeartbeat(Instant.now());
                worker.setStatus(WorkerStatus.ONLINE);
                workerRepo.save(worker);
                return true;
            })
            .orElse(false);
    }

    @Override
    public boolean deregisterWorker(UUID workerId) {
        return workerRepo.findById(workerId)
            .map(worker -> {
                worker.setStatus(WorkerStatus.OFFLINE);
                workerRepo.save(worker);
                return true;
            })
            .orElse(false);
    }

    // Passive detection for workers that stop heartbeating without ever calling deregister
    // (crash, kill, network partition) — the only way scheduler-api can notice, since workers
    // push heartbeats and are never polled. Two thresholds, not one: a single missed heartbeat
    // shouldn't immediately declare a worker dead (could just be a GC pause or transient network
    // blip) — UNHEALTHY first, OFFLINE only after a much longer silence. Recovery back to ONLINE
    // happens for free in heartbeatWorker above whenever the worker heartbeats again.
    //
    // Scoped to ONLINE/UNHEALTHY workers only — once a worker is OFFLINE it's a terminal, historical
    // record (registration always inserts a new row per process instance, never upserts), so there's
    // no reason to keep re-checking it on every sweep.
    @Scheduled(fixedDelayString = "${worker.health.sweep-interval-ms:30000}")
    public void sweepStaleWorkers() {
        Instant now = Instant.now();
        Instant unhealthyThreshold = now.minusMillis(unhealthyAfterMs);
        Instant offlineThreshold = now.minusMillis(offlineAfterMs);

        for (Worker worker : workerRepo.findByStatusIn(List.of(WorkerStatus.ONLINE, WorkerStatus.UNHEALTHY))) {
            if (worker.getLastHeartbeat().isBefore(offlineThreshold)) {
                worker.setStatus(WorkerStatus.OFFLINE);
                workerRepo.save(worker);
            } else if (worker.getStatus() == WorkerStatus.ONLINE && worker.getLastHeartbeat().isBefore(unhealthyThreshold)) {
                worker.setStatus(WorkerStatus.UNHEALTHY);
                workerRepo.save(worker);
            }
        }
    }

    @Override
    public List<Worker> getWorkers() {
        return workerRepo.findAll();
    }

    @Override
    public List<Worker> getWorkersByType(QueueType queueType) {
        return workerRepo.findByQueueType(queueType);
    }

    @Override
    @SuppressWarnings("null")
    public List<TaskExecution> getTaskExecutionsByWorkerType(QueueType queueType) {
        List<UUID> workerIds = workerRepo.findByQueueType(queueType).stream()
            .map(Worker::getId)
            .toList();

        return workerIds.isEmpty() ? List.of() : taskExecutionRepo.findByWorkerIdIn(workerIds);
    }
}
