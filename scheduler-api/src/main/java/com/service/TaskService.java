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

import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.MessageMetadata;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.requests.PutMessagesRequest;

import com.DAO.TaskDAO;
import com.entity.Plan;
import com.entity.Task;
import com.enums.Enums.PriorityType;
import com.enums.Enums.QueueType;
import com.enums.Enums.TaskStatus;
import com.enums.Enums.TaskType;
import com.exception.RateLimitExceededException;
import com.oci.QueueConnectionManager;
import com.repo.PlanRepo;
import com.repo.TaskRepo;
import com.repo.TenantMetricsRepo;
import com.repo.TenantRepo;

@Service
public class TaskService implements TaskDAO{
    private final TaskRepo taskRepo;
    private final QueueConnectionManager queueConnectionManager;
    private final TenantRepo tenantRepo;
    private final PlanRepo planRepo;
    private final RateLimiterService rateLimiterService;
    private final TenantMetricsRepo tenantMetricsRepo;

    public TaskService(TaskRepo taskRepo, QueueConnectionManager queueConnectionManager,
            TenantRepo tenantRepo, PlanRepo planRepo, RateLimiterService rateLimiterService,
            TenantMetricsRepo tenantMetricsRepo) {
        this.taskRepo = taskRepo;
        this.queueConnectionManager = queueConnectionManager;
        this.tenantRepo = tenantRepo;
        this.planRepo = planRepo;
        this.rateLimiterService = rateLimiterService;
        this.tenantMetricsRepo = tenantMetricsRepo;
    }

    @Override
    public UUID queueTask(UUID tenantId, TaskType type, PriorityType priority, Instant scheduledAt) {
        enforceRateLimit(tenantId);

        Task task = new Task();
        task.setTenantId(tenantId);
        task.setTaskType(type);
        task.setPriority(priority);
        task.setScheduledAt(scheduledAt);
        task.setTaskStatus(TaskStatus.QUEUED);
        task.setCreatedAt(Instant.now());
        UUID taskId = taskRepo.save(task).getId();

        publishToQueue(tenantId, priority, taskId);

        return taskId;
    }

    private void enforceRateLimit(UUID tenantId) {
        Optional<Integer> rateLimit = tenantRepo.findById(tenantId)
                .flatMap(tenant -> planRepo.findById(tenant.getPlanId()))
                .map(Plan::getRateLimit);

        if (rateLimit.isEmpty()) {
            return; // unresolvable tenant/plan — let downstream (e.g. the tasks.tenant_id FK) handle it
        }

        if (!rateLimiterService.tryAcquire(tenantId, rateLimit.get())) {
            tenantMetricsRepo.incrementRateLimitHits(tenantId);
            throw new RateLimitExceededException(tenantId);
        }
    }

    // Tenant is tagged as the message's channel — see the earlier design discussion on using
    // OCI Queue's channel/max-channel-consumption fairness feature to stop one busy tenant from
    // starving others on a shared queue.
    private void publishToQueue(UUID tenantId, PriorityType priority, UUID taskId) {
        QueueType queueType = priority == PriorityType.HIGH_PRIORITY ? QueueType.HIGH_PRIORITY : QueueType.DEFAULT_PRIORITY;
        QueueClient client = queueConnectionManager.getClient(queueType);
        String queueId = queueConnectionManager.getQueueId(queueType);

        PutMessagesDetailsEntry entry = PutMessagesDetailsEntry.builder()
                .content(taskId.toString())
                .metadata(MessageMetadata.builder()
                        .channelId(tenantId.toString())
                        .build())
                .build();

        PutMessagesRequest request = PutMessagesRequest.builder()
                .queueId(queueId)
                .putMessagesDetails(PutMessagesDetails.builder()
                        .messages(List.of(entry))
                        .build())
                .build();

        client.putMessages(request);
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
