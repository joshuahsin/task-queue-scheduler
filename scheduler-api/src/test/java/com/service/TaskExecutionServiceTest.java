package com.service;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.entity.TaskExecution;
import com.enums.Enums.TaskExecutionStatus;
import com.repo.TaskExecutionRepo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskExecutionServiceTest {

    @Mock
    private TaskExecutionRepo taskExecutionRepo;

    private TaskExecutionService taskExecutionService;

    @BeforeEach
    void setUp() {
        taskExecutionService = new TaskExecutionService(taskExecutionRepo);
    }

    @Test
    void getTaskExecutions_delegatesToRepo() {
        UUID taskId = UUID.randomUUID();
        TaskExecution execution = new TaskExecution();
        execution.setTaskId(taskId);
        when(taskExecutionRepo.findByTaskId(taskId)).thenReturn(List.of(execution));

        List<TaskExecution> result = taskExecutionService.getTaskExecutions(taskId);

        assertThat(result).containsExactly(execution);
    }

    @Test
    void cancelTaskExecution_cancelsOnlyTheRunningExecutionsForThatTask() {
        UUID taskId = UUID.randomUUID();
        TaskExecution running = new TaskExecution();
        running.setTaskId(taskId);
        running.setStatus(TaskExecutionStatus.RUNNING);
        TaskExecution alreadySucceeded = new TaskExecution();
        alreadySucceeded.setTaskId(taskId);
        alreadySucceeded.setStatus(TaskExecutionStatus.SUCCESS);
        when(taskExecutionRepo.findByTaskId(taskId)).thenReturn(List.of(running, alreadySucceeded));

        boolean result = taskExecutionService.cancelTaskExecution(taskId);

        assertThat(result).isTrue();
        assertThat(running.getStatus()).isEqualTo(TaskExecutionStatus.CANCELLED);
        assertThat(alreadySucceeded.getStatus()).isEqualTo(TaskExecutionStatus.SUCCESS);
        verify(taskExecutionRepo).saveAll(List.of(running));
    }

    @Test
    void cancelTaskExecution_returnsFalseWhenNoExecutionIsRunning() {
        UUID taskId = UUID.randomUUID();
        TaskExecution succeeded = new TaskExecution();
        succeeded.setTaskId(taskId);
        succeeded.setStatus(TaskExecutionStatus.SUCCESS);
        when(taskExecutionRepo.findByTaskId(taskId)).thenReturn(List.of(succeeded));

        boolean result = taskExecutionService.cancelTaskExecution(taskId);

        assertThat(result).isFalse();
        verify(taskExecutionRepo, never()).saveAll(org.mockito.ArgumentMatchers.<List<TaskExecution>>any());
    }

    @Test
    void cancelTaskExecution_returnsFalseWhenTaskHasNoExecutionsAtAll() {
        UUID taskId = UUID.randomUUID();
        when(taskExecutionRepo.findByTaskId(taskId)).thenReturn(List.of());

        boolean result = taskExecutionService.cancelTaskExecution(taskId);

        assertThat(result).isFalse();
    }
}
