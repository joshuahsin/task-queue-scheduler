package com.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.DAO.TaskDAO;
import com.entity.Task;
import com.enums.Enums.PriorityType;
import com.enums.Enums.TaskStatus;
import com.enums.Enums.TaskType;
import com.repo.TaskRepo;

@Service
public class TaskService implements TaskDAO{
    private final TaskRepo taskRepo;

    public TaskService(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
    }

    @Override
    public UUID queueTask(UUID tenantId, TaskType type, PriorityType priority, Instant scheduledAt) {
        Task task = new Task();
        task.setTenantId(tenantId);
        task.setTaskType(type);
        task.setPriority(priority);
        task.setScheduledAt(scheduledAt);
        task.setTaskStatus(TaskStatus.QUEUED);
        task.setCreatedAt(Instant.now());
        return taskRepo.save(task).getId();
    }

    @Override
    public boolean cancelTask(UUID taskId, UUID tenantId) {
        return taskRepo.findById(taskId)
            .filter(task -> task.getTenantId().equals(tenantId))
            .map(task -> {
                task.setTaskStatus(TaskStatus.CANCELLED);
                taskRepo.save(task);
                return true;
            })
            .orElse(false);
    }

    @Override
    public Optional<Task> getTask(UUID taskId, UUID tenantId) {
        return taskRepo.findById(taskId)
            .filter(task -> task.getTenantId().equals(tenantId));
    }

    @Override
    public Page<Task> getTasksQuery(UUID tenantId, int priority, TaskStatus status, int page, int size) {
        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (priority >= 0) {
                predicates.add(cb.equal(root.get("priority"), PriorityType.values()[priority]));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("taskStatus"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size);
        return taskRepo.findAll(spec, pageable);
    }
}
