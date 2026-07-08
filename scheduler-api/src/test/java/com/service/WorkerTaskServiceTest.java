package com.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.DTO.ClaimTaskResponse;
import com.entity.Task;
import com.entity.TaskExecution;
import com.enums.Enums.ClaimOutcome;
import com.enums.Enums.ErrorType;
import com.enums.Enums.TaskExecutionStatus;
import com.enums.Enums.TaskStatus;
import com.exception.TaskExecutionNotFoundException;
import com.exception.WorkerNotFoundException;
import com.repo.TaskExecutionRepo;
import com.repo.TaskRepo;
import com.repo.WorkerRepo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerTaskServiceTest {

    @Mock
    private TaskRepo taskRepo;
    @Mock
    private TaskExecutionRepo taskExecutionRepo;
    @Mock
    private WorkerRepo workerRepo;

    private WorkerTaskService workerTaskService;

    @BeforeEach
    void setUp() {
        workerTaskService = new WorkerTaskService(taskRepo, taskExecutionRepo, workerRepo);
    }

    @Test
    void claimTask_throwsWhenWorkerNotRegistered() {
        UUID workerId = UUID.randomUUID();
        when(workerRepo.existsById(workerId)).thenReturn(false);

        assertThatThrownBy(() -> workerTaskService.claimTask(workerId, UUID.randomUUID()))
                .isInstanceOf(WorkerNotFoundException.class);

        verify(taskRepo, never()).findById(any());
    }

    @Test
    void claimTask_skipsWhenTaskNotFound() {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(workerRepo.existsById(workerId)).thenReturn(true);
        when(taskRepo.findById(taskId)).thenReturn(Optional.empty());

        ClaimTaskResponse response = workerTaskService.claimTask(workerId, taskId);

        assertThat(response.getOutcome()).isEqualTo(ClaimOutcome.SKIPPED);
        assertThat(response.getSkipReason()).isEqualTo("TASK_NOT_FOUND");
        verify(taskExecutionRepo, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"CANCELLED", "SUCCESS", "DEAD"})
    void claimTask_skipsTerminalTasks(TaskStatus status) {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTaskStatus(status);
        when(workerRepo.existsById(workerId)).thenReturn(true);
        when(taskRepo.findById(taskId)).thenReturn(Optional.of(task));

        ClaimTaskResponse response = workerTaskService.claimTask(workerId, taskId);

        assertThat(response.getOutcome()).isEqualTo(ClaimOutcome.SKIPPED);
        assertThat(response.getSkipReason()).isEqualTo("TASK_" + status.name());
        verify(taskRepo, never()).save(any());
        verify(taskExecutionRepo, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"QUEUED", "RUNNING", "FAILED", "RETRYING"})
    void claimTask_claimsRunnableTasksAndStampsWorkerId(TaskStatus status) {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTaskStatus(status);
        task.setTaskType(com.enums.Enums.TaskType.SEND_EMAIL);
        when(workerRepo.existsById(workerId)).thenReturn(true);
        when(taskRepo.findById(taskId)).thenReturn(Optional.of(task));
        when(taskExecutionRepo.findByTaskId(taskId)).thenReturn(List.of(new TaskExecution(), new TaskExecution()));

        ClaimTaskResponse response = workerTaskService.claimTask(workerId, taskId);

        assertThat(response.getOutcome()).isEqualTo(ClaimOutcome.CLAIMED);
        assertThat(response.getAttemptNumber()).isEqualTo(3);
        assertThat(response.getTaskType()).isEqualTo(com.enums.Enums.TaskType.SEND_EMAIL);
        assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.RUNNING);
        verify(taskRepo).save(task);

        ArgumentCaptor<TaskExecution> executionCaptor = ArgumentCaptor.forClass(TaskExecution.class);
        verify(taskExecutionRepo).save(executionCaptor.capture());
        TaskExecution savedExecution = executionCaptor.getValue();
        assertThat(savedExecution.getTaskId()).isEqualTo(taskId);
        assertThat(savedExecution.getWorkerId()).isEqualTo(workerId);
        assertThat(savedExecution.getAttemptNumber()).isEqualTo(3);
        assertThat(savedExecution.getStatus()).isEqualTo(TaskExecutionStatus.RUNNING);
        assertThat(savedExecution.getStartedAt()).isNotNull();
    }

    @Test
    void completeExecution_throwsWhenExecutionNotFound() {
        UUID executionId = UUID.randomUUID();
        when(taskExecutionRepo.findById(executionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workerTaskService.completeExecution(
                UUID.randomUUID(), UUID.randomUUID(), executionId, true, null, null))
                .isInstanceOf(TaskExecutionNotFoundException.class);
    }

    @Test
    void completeExecution_throwsWhenExecutionBelongsToDifferentTask() {
        UUID executionId = UUID.randomUUID();
        TaskExecution execution = new TaskExecution();
        execution.setId(executionId);
        execution.setTaskId(UUID.randomUUID());
        when(taskExecutionRepo.findById(executionId)).thenReturn(Optional.of(execution));

        assertThatThrownBy(() -> workerTaskService.completeExecution(
                UUID.randomUUID(), UUID.randomUUID(), executionId, true, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completeExecution_onSuccessMarksExecutionAndTaskSuccessful() {
        UUID taskId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        TaskExecution execution = new TaskExecution();
        execution.setId(executionId);
        execution.setTaskId(taskId);
        Task task = new Task();
        task.setId(taskId);
        task.setRetryCount(0);
        when(taskExecutionRepo.findById(executionId)).thenReturn(Optional.of(execution));
        when(taskRepo.findById(taskId)).thenReturn(Optional.of(task));

        workerTaskService.completeExecution(UUID.randomUUID(), taskId, executionId, true, null, null);

        assertThat(execution.getStatus()).isEqualTo(TaskExecutionStatus.SUCCESS);
        assertThat(execution.getFinishedAt()).isNotNull();
        assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.SUCCESS);
        assertThat(task.getRetryCount()).isZero();
        verify(taskExecutionRepo).save(execution);
        verify(taskRepo).save(task);
    }

    @Test
    void completeExecution_onFailureMarksExecutionFailedAndIncrementsRetryCount() {
        UUID taskId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        TaskExecution execution = new TaskExecution();
        execution.setId(executionId);
        execution.setTaskId(taskId);
        Task task = new Task();
        task.setId(taskId);
        task.setRetryCount(1);
        when(taskExecutionRepo.findById(executionId)).thenReturn(Optional.of(execution));
        when(taskRepo.findById(taskId)).thenReturn(Optional.of(task));

        workerTaskService.completeExecution(UUID.randomUUID(), taskId, executionId, false, ErrorType.TRANSIENT, "boom");

        assertThat(execution.getStatus()).isEqualTo(TaskExecutionStatus.FAILED);
        assertThat(execution.getErrorType()).isEqualTo(ErrorType.TRANSIENT);
        assertThat(execution.getErrorMessage()).isEqualTo("boom");
        assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.RETRYING);
        assertThat(task.getRetryCount()).isEqualTo(2);
    }
}
