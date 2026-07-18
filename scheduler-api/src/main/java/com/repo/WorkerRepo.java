package com.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.entity.Worker;
import com.enums.Enums.QueueType;
import com.enums.Enums.WorkerStatus;

public interface WorkerRepo extends JpaRepository<Worker, UUID> {
    List<Worker> findByQueueType(QueueType queueType);
    List<Worker> findByStatusIn(List<WorkerStatus> statuses);
}