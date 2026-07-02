package com.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<UUID> queueTask(@Valid @RequestBody QueueTaskRequest request) {
        UUID taskId = taskService.queueTask(request.getType(), request.getPriority(), request.getScheduledAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(taskId);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID taskId) {
        return ResponseEntity.ok(TaskResponse.from(taskService.getTask(taskId)));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> cancelTask(@PathVariable UUID taskId) {
        taskService.cancelTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasksQuery(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) PriorityType priority,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "-1") int retryCount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TaskResponse> results = taskService
            .getTasksQuery(tenantId, priority, status, retryCount, page, size)
            .map(TaskResponse::from);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{taskId}/executions")
    public ResponseEntity<List<TaskExecutionResponse>> getTaskExecutions(@PathVariable UUID taskId) {
        List<TaskExecutionResponse> executions = taskExecutionService.getTaskExecutions(taskId)
            .stream()
            .map(TaskExecutionResponse::from)
            .toList();
        return ResponseEntity.ok(executions);
    }

    @DeleteMapping("/{taskId}/executions")
    public ResponseEntity<Void> cancelTaskExecution(@PathVariable UUID taskId) {
        taskExecutionService.cancelTaskExecution(taskId);
        return ResponseEntity.noContent().build();
    }
}
