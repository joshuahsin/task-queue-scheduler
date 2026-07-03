package com.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    public WorkerService(WorkerRepo workerRepo, TaskExecutionRepo taskExecutionRepo) {
        this.workerRepo = workerRepo;
        this.taskExecutionRepo = taskExecutionRepo;
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
    public List<Worker> getWorkers() {
        return workerRepo.findAll();
    }

    @Override
    public List<Worker> getWorkersByType(QueueType queueType) {
        return workerRepo.findByQueueType(queueType);
    }

    @Override
    public List<TaskExecution> getTaskExecutionsByWorkerType(QueueType queueType) {
        List<UUID> workerIds = workerRepo.findByQueueType(queueType).stream()
            .map(Worker::getId)
            .toList();

        return workerIds.isEmpty() ? List.of() : taskExecutionRepo.findByWorkerIdIn(workerIds);
    }
}
