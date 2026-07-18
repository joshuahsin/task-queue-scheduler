package com.DAO;

import java.util.List;
import java.util.UUID;

import com.entity.TaskExecution;
import com.entity.Worker;

import com.enums.Enums.QueueType;

public interface WorkerDAO {
    public Worker registerWorker(String name, QueueType queueType, String hostname, int pid, int capacity, String version);
    public boolean heartbeatWorker(UUID workerId);
    public boolean deregisterWorker(UUID workerId);
    public List<Worker> getWorkers();
    public List<Worker> getWorkersByType(QueueType queueType);
    public List<TaskExecution> getTaskExecutionsByWorkerType(QueueType queueType);
}
