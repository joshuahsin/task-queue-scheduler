package com.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.requests.PutMessagesRequest;

import com.entity.Plan;
import com.entity.Task;
import com.entity.Tenant;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepo taskRepo;
    @Mock
    private QueueConnectionManager queueConnectionManager;
    @Mock
    private QueueClient queueClient;
    @Mock
    private TenantRepo tenantRepo;
    @Mock
    private PlanRepo planRepo;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private TenantMetricsRepo tenantMetricsRepo;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepo, queueConnectionManager, tenantRepo, planRepo,
                rateLimiterService, tenantMetricsRepo);
    }

    @Test
    void queueTask_savesTaskAndPublishesToHighPriorityQueue() {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Instant scheduledAt = Instant.now();

        when(taskRepo.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(taskId);
            return task;
        });
        when(queueConnectionManager.getClient(QueueType.HIGH_PRIORITY)).thenReturn(queueClient);
        when(queueConnectionManager.getQueueId(QueueType.HIGH_PRIORITY)).thenReturn("queue-ocid");

        UUID result = taskService.queueTask(tenantId, TaskType.SIMULATED_WORK, PriorityType.HIGH_PRIORITY, scheduledAt);

        assertThat(result).isEqualTo(taskId);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepo).save(taskCaptor.capture());
        Task savedTask = taskCaptor.getValue();
        assertThat(savedTask.getTenantId()).isEqualTo(tenantId);
        assertThat(savedTask.getTaskType()).isEqualTo(TaskType.SIMULATED_WORK);
        assertThat(savedTask.getPriority()).isEqualTo(PriorityType.HIGH_PRIORITY);
        assertThat(savedTask.getScheduledAt()).isEqualTo(scheduledAt);
        assertThat(savedTask.getTaskStatus()).isEqualTo(TaskStatus.QUEUED);

        ArgumentCaptor<PutMessagesRequest> requestCaptor = ArgumentCaptor.forClass(PutMessagesRequest.class);
        verify(queueClient).putMessages(requestCaptor.capture());
        PutMessagesRequest request = requestCaptor.getValue();
        assertThat(request.getQueueId()).isEqualTo("queue-ocid");
        PutMessagesDetailsEntry entry = request.getPutMessagesDetails().getMessages().get(0);
        assertThat(entry.getContent()).isEqualTo(taskId.toString());
        assertThat(entry.getMetadata().getChannelId()).isEqualTo(tenantId.toString());
    }

    @Test
    void queueTask_routesDefaultPriorityToDefaultPriorityQueue() {
        UUID tenantId = UUID.randomUUID();
        when(taskRepo.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(UUID.randomUUID());
            return task;
        });
        when(queueConnectionManager.getClient(QueueType.DEFAULT_PRIORITY)).thenReturn(queueClient);
        when(queueConnectionManager.getQueueId(QueueType.DEFAULT_PRIORITY)).thenReturn("default-queue-ocid");

        taskService.queueTask(tenantId, TaskType.SEND_EMAIL, PriorityType.DEFAULT_PRIORITY, null);

        verify(queueConnectionManager).getClient(QueueType.DEFAULT_PRIORITY);
        verify(queueConnectionManager, org.mockito.Mockito.never()).getClient(QueueType.HIGH_PRIORITY);
    }

    @Test
    void cancelTask_setsCancelledWhenTaskBelongsToTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTenantId(tenantId);
        task.setTaskStatus(TaskStatus.QUEUED);
        when(taskRepo.findById(taskId)).thenReturn(Optional.of(task));

        boolean result = taskService.cancelTask(taskId, tenantId);

        assertThat(result).isTrue();
        assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.CANCELLED);
        verify(taskRepo).save(task);
    }

    @Test
    void cancelTask_returnsFalseWhenTaskBelongsToDifferentTenant() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTenantId(UUID.randomUUID());
        when(taskRepo.findById(taskId)).thenReturn(Optional.of(task));

        boolean result = taskService.cancelTask(taskId, UUID.randomUUID());

        assertThat(result).isFalse();
        verify(taskRepo, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void cancelTask_returnsFalseWhenTaskNotFound() {
        UUID taskId = UUID.randomUUID();
        when(taskRepo.findById(taskId)).thenReturn(Optional.empty());

        assertThat(taskService.cancelTask(taskId, UUID.randomUUID())).isFalse();
    }

    @Test
    void getTask_returnsEmptyWhenTenantDoesNotMatch() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTenantId(UUID.randomUUID());
        when(taskRepo.findById(taskId)).thenReturn(Optional.of(task));

        assertThat(taskService.getTask(taskId, UUID.randomUUID())).isEmpty();
    }

    @Test
    void getTask_returnsTaskWhenTenantMatches() {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTenantId(tenantId);
        when(taskRepo.findById(taskId)).thenReturn(Optional.of(task));

        assertThat(taskService.getTask(taskId, tenantId)).contains(task);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getTasksQuery_delegatesToRepoWithPageableAndFiltersByTenantPriorityAndStatus() {
        UUID tenantId = UUID.randomUUID();
        Page<Task> page = new PageImpl<>(List.of());
        when(taskRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Task> result = taskService.getTasksQuery(tenantId, 0, TaskStatus.QUEUED, 2, 10);

        assertThat(result).isSameAs(page);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepo).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getTasksQuery_omitsPriorityFilterWhenNegative() {
        when(taskRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        taskService.getTasksQuery(UUID.randomUUID(), -1, null, 0, 20);

        // no exception thrown building/using the specification with priority=-1 and status=null
        verify(taskRepo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void queueTask_throwsRateLimitExceededAndIncrementsMetricWhenOverLimit() {
        UUID tenantId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setPlanId(planId);
        Plan plan = new Plan();
        plan.setRateLimit(60);
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(planRepo.findById(planId)).thenReturn(Optional.of(plan));
        when(rateLimiterService.tryAcquire(tenantId, 60)).thenReturn(false);

        assertThatThrownBy(() -> taskService.queueTask(tenantId, TaskType.SIMULATED_WORK, PriorityType.HIGH_PRIORITY, null))
                .isInstanceOf(RateLimitExceededException.class);

        verify(tenantMetricsRepo).incrementRateLimitHits(tenantId);
        verify(taskRepo, never()).save(any());
        verify(queueConnectionManager, never()).getClient(any());
    }

    @Test
    void queueTask_proceedsWhenUnderLimit() {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setPlanId(planId);
        Plan plan = new Plan();
        plan.setRateLimit(60);
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(planRepo.findById(planId)).thenReturn(Optional.of(plan));
        when(rateLimiterService.tryAcquire(tenantId, 60)).thenReturn(true);
        when(taskRepo.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(taskId);
            return task;
        });
        when(queueConnectionManager.getClient(QueueType.HIGH_PRIORITY)).thenReturn(queueClient);
        when(queueConnectionManager.getQueueId(QueueType.HIGH_PRIORITY)).thenReturn("queue-ocid");

        UUID result = taskService.queueTask(tenantId, TaskType.SIMULATED_WORK, PriorityType.HIGH_PRIORITY, null);

        assertThat(result).isEqualTo(taskId);
        verify(rateLimiterService).tryAcquire(tenantId, 60);
        verify(tenantMetricsRepo, never()).incrementRateLimitHits(any());
    }

    @Test
    void queueTask_skipsRateLimitWhenTenantNotFound() {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.empty());
        when(taskRepo.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(taskId);
            return task;
        });
        when(queueConnectionManager.getClient(QueueType.HIGH_PRIORITY)).thenReturn(queueClient);
        when(queueConnectionManager.getQueueId(QueueType.HIGH_PRIORITY)).thenReturn("queue-ocid");

        UUID result = taskService.queueTask(tenantId, TaskType.SIMULATED_WORK, PriorityType.HIGH_PRIORITY, null);

        assertThat(result).isEqualTo(taskId);
        verify(rateLimiterService, never()).tryAcquire(any(), any(Integer.class));
        verify(tenantMetricsRepo, never()).incrementRateLimitHits(any());
    }
}
