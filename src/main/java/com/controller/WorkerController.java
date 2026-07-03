package com.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.DTO.RegisterWorkerRequest;
import com.DTO.TaskExecutionResponse;
import com.DTO.WorkerResponse;
import com.enums.Enums.QueueType;
import com.exception.WorkerNotFoundException;
import com.service.WorkerService;

@RestController
@RequestMapping("/api/v1/workers")
public class WorkerController {
    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @PostMapping
    public ResponseEntity<WorkerResponse> registerWorker(@Valid @RequestBody RegisterWorkerRequest request) {
        var worker = workerService.registerWorker(
            request.getName(), request.getQueueType(), request.getHostname(),
            request.getPid(), request.getCapacity(), request.getVersion()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkerResponse.from(worker));
    }

    @PostMapping("/{workerId}/heartbeat")
    public ResponseEntity<Void> heartbeatWorker(@PathVariable UUID workerId) {
        if (!workerService.heartbeatWorker(workerId)) {
            throw new WorkerNotFoundException(workerId);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<WorkerResponse>> getWorkers(@RequestParam(required = false) QueueType queueType) {
        List<WorkerResponse> workers = (queueType != null ? workerService.getWorkersByType(queueType) : workerService.getWorkers())
            .stream()
            .map(WorkerResponse::from)
            .toList();
        return ResponseEntity.ok(workers);
    }

    @GetMapping("/executions")
    public ResponseEntity<List<TaskExecutionResponse>> getTaskExecutionsByWorkerType(@RequestParam QueueType queueType) {
        List<TaskExecutionResponse> executions = workerService.getTaskExecutionsByWorkerType(queueType).stream()
            .map(TaskExecutionResponse::from)
            .toList();
        return ResponseEntity.ok(executions);
    }
}
