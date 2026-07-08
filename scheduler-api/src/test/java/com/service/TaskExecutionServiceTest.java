package com.service;

import java.util.List;
import java.util.Optional;
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
    void cancelTaskExecution_setsCancelledStatusWhenFound() {
        UUID executionId = UUID.randomUUID();
        TaskExecution execution = new TaskExecution();
        execution.setId(executionId);
        execution.setStatus(TaskExecutionStatus.RUNNING);
        when(taskExecutionRepo.findById(executionId)).thenReturn(Optional.of(execution));

        boolean result = taskExecutionService.cancelTaskExecution(executionId);

        assertThat(result).isTrue();
        assertThat(execution.getStatus()).isEqualTo(TaskExecutionStatus.CANCELLED);
        verify(taskExecutionRepo).save(execution);
    }

    @Test
    void cancelTaskExecution_returnsFalseWhenNotFound() {
        UUID executionId = UUID.randomUUID();
        when(taskExecutionRepo.findById(executionId)).thenReturn(Optional.empty());

        boolean result = taskExecutionService.cancelTaskExecution(executionId);

        assertThat(result).isFalse();
        verify(taskExecutionRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
