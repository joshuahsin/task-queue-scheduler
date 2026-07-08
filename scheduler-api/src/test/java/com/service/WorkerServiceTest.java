package com.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.entity.TaskExecution;
import com.entity.Worker;
import com.enums.Enums.QueueType;
import com.enums.Enums.WorkerStatus;
import com.repo.TaskExecutionRepo;
import com.repo.WorkerRepo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock
    private WorkerRepo workerRepo;
    @Mock
    private TaskExecutionRepo taskExecutionRepo;

    private WorkerService workerService;

    @BeforeEach
    void setUp() {
        workerService = new WorkerService(workerRepo, taskExecutionRepo);
    }

    @Test
    void registerWorker_savesOnlineWorkerWithGivenFields() {
        when(workerRepo.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Worker worker = workerService.registerWorker("worker-1", QueueType.HIGH_PRIORITY, "host-1", 123, 4, "1.0.0");

        assertThat(worker.getName()).isEqualTo("worker-1");
        assertThat(worker.getQueueType()).isEqualTo(QueueType.HIGH_PRIORITY);
        assertThat(worker.getHostname()).isEqualTo("host-1");
        assertThat(worker.getPid()).isEqualTo(123);
        assertThat(worker.getCapacity()).isEqualTo(4);
        assertThat(worker.getVersion()).isEqualTo("1.0.0");
        assertThat(worker.getStatus()).isEqualTo(WorkerStatus.ONLINE);
        assertThat(worker.getStartedAt()).isNotNull();
        assertThat(worker.getLastHeartbeat()).isEqualTo(worker.getStartedAt());
    }

    @Test
    void heartbeatWorker_updatesLastHeartbeatAndStatusWhenFound() {
        UUID workerId = UUID.randomUUID();
        Worker worker = new Worker();
        worker.setId(workerId);
        worker.setStatus(WorkerStatus.UNHEALTHY);
        when(workerRepo.findById(workerId)).thenReturn(Optional.of(worker));

        boolean result = workerService.heartbeatWorker(workerId);

        assertThat(result).isTrue();
        assertThat(worker.getStatus()).isEqualTo(WorkerStatus.ONLINE);
        assertThat(worker.getLastHeartbeat()).isNotNull();
        verify(workerRepo).save(worker);
    }

    @Test
    void heartbeatWorker_returnsFalseWhenNotFound() {
        UUID workerId = UUID.randomUUID();
        when(workerRepo.findById(workerId)).thenReturn(Optional.empty());

        assertThat(workerService.heartbeatWorker(workerId)).isFalse();
        verify(workerRepo, never()).save(any());
    }

    @Test
    void getWorkers_delegatesToRepo() {
        Worker worker = new Worker();
        when(workerRepo.findAll()).thenReturn(List.of(worker));

        assertThat(workerService.getWorkers()).containsExactly(worker);
    }

    @Test
    void getWorkersByType_delegatesToRepo() {
        Worker worker = new Worker();
        when(workerRepo.findByQueueType(QueueType.DEFAULT_PRIORITY)).thenReturn(List.of(worker));

        assertThat(workerService.getWorkersByType(QueueType.DEFAULT_PRIORITY)).containsExactly(worker);
    }

    @Test
    void getTaskExecutionsByWorkerType_returnsEmptyWhenNoWorkersOfThatType() {
        when(workerRepo.findByQueueType(QueueType.HIGH_PRIORITY)).thenReturn(List.of());

        List<TaskExecution> result = workerService.getTaskExecutionsByWorkerType(QueueType.HIGH_PRIORITY);

        assertThat(result).isEmpty();
        verify(taskExecutionRepo, never()).findByWorkerIdIn(any());
    }

    @Test
    void getTaskExecutionsByWorkerType_looksUpExecutionsForMatchingWorkerIds() {
        UUID workerId = UUID.randomUUID();
        Worker worker = new Worker();
        worker.setId(workerId);
        when(workerRepo.findByQueueType(QueueType.HIGH_PRIORITY)).thenReturn(List.of(worker));
        TaskExecution execution = new TaskExecution();
        when(taskExecutionRepo.findByWorkerIdIn(List.of(workerId))).thenReturn(List.of(execution));

        List<TaskExecution> result = workerService.getTaskExecutionsByWorkerType(QueueType.HIGH_PRIORITY);

        assertThat(result).containsExactly(execution);
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskExecutionRepo).findByWorkerIdIn(captor.capture());
        assertThat(captor.getValue()).containsExactly(workerId);
    }
}
