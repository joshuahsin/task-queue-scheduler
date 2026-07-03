package com.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.DTO.QueueTaskRequest;
import com.DTO.TaskExecutionResponse;
import com.DTO.TaskResponse;
import com.enums.Enums.PriorityType;
import com.enums.Enums.TaskStatus;
import com.exception.TaskExecutionNotFoundException;
import com.exception.TaskNotFoundException;
import com.security.AuthenticatedPrincipal;
import com.service.TaskExecutionService;
import com.service.TaskService;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskService taskService;
    private final TaskExecutionService taskExecutionService;

    public TaskController(TaskService taskService, TaskExecutionService taskExecutionService) {
        this.taskService = taskService;
        this.taskExecutionService = taskExecutionService;
    }

    @PostMapping
    public ResponseEntity<UUID> queueTask(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody QueueTaskRequest request) {
        UUID taskId = taskService.queueTask(principal.tenantId(), request.getType(), request.getPriority(), request.getScheduledAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(taskId);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId) {
        TaskResponse response = taskService.getTask(taskId, principal.tenantId())
            .map(TaskResponse::from)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> cancelTask(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId) {
        if (!taskService.cancelTask(taskId, principal.tenantId())) {
            throw new TaskNotFoundException(taskId);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasksQuery(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam(required = false) PriorityType priority,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TaskResponse> results = taskService
            .getTasksQuery(principal.tenantId(), priority != null ? priority.ordinal() : -1, status, page, size)
            .map(TaskResponse::from);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{taskId}/executions")
    public ResponseEntity<List<TaskExecutionResponse>> getTaskExecutions(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId) {
        // Verifies the task belongs to the caller's tenant before exposing its executions.
        taskService.getTask(taskId, principal.tenantId()).orElseThrow(() -> new TaskNotFoundException(taskId));

        List<TaskExecutionResponse> executions = taskExecutionService.getTaskExecutions(taskId)
            .stream()
            .map(TaskExecutionResponse::from)
            .toList();
        return ResponseEntity.ok(executions);
    }

    @DeleteMapping("/{taskId}/executions")
    public ResponseEntity<Void> cancelTaskExecution(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId) {
        taskService.getTask(taskId, principal.tenantId()).orElseThrow(() -> new TaskNotFoundException(taskId));

        if (!taskExecutionService.cancelTaskExecution(taskId)) {
            throw new TaskExecutionNotFoundException(taskId);
        }
        return ResponseEntity.noContent().build();
    }
}
