package com.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import com.entity.TaskExecution;

import java.util.List;
import java.util.UUID;

public interface TaskExecutionRepo extends JpaRepository<TaskExecution, UUID> {
    List<TaskExecution> findByTaskId(UUID taskId);
    List<TaskExecution> findByTaskIdIn(List<UUID> taskIds);
    List<TaskExecution> findByWorkerIdIn(List<UUID> workerIds);
}
