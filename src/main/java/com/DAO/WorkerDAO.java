package com.DAO;

import java.util.List;

import com.entity.TaskExecution;
import com.entity.Worker;

import com.enums.Enums.QueueType;

public interface WorkerDAO {
    public Worker registerWorker(String name, QueueType queueType);
    public boolean heartbeatWorker(String workerId);
    public List<Worker> getWorkers();
    public List<Worker> getWorkersByType(QueueType queueType);
    public List<TaskExecution> getTaskExecutionsByWorkerType(QueueType queueType);
}
