package com.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;

import com.DTO.ClaimTaskResponse;
import com.entity.TaskExecution;
import com.entity.Worker;
import com.enums.Enums.QueueType;
import com.enums.Enums.TaskExecutionStatus;
import com.enums.Enums.TaskType;
import com.enums.Enums.WorkerStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkerController.class)
class WorkerControllerTest extends AbstractControllerTest {

    @Test
    void registerWorker_returns201WithWorkerResponse() throws Exception {
        Worker worker = new Worker();
        worker.setId(UUID.randomUUID());
        worker.setName("worker-1");
        worker.setQueueType(QueueType.HIGH_PRIORITY);
        worker.setStatus(WorkerStatus.ONLINE);
        when(workerService.registerWorker("worker-1", QueueType.HIGH_PRIORITY, "host-1", 123, 4, "1.0.0"))
                .thenReturn(worker);

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", adminToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"worker-1","queueType":"HIGH_PRIORITY","hostname":"host-1","pid":123,"capacity":4,"version":"1.0.0"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(worker.getId().toString()))
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    void registerWorker_returns400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", adminToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","queueType":"HIGH_PRIORITY","hostname":"host-1","pid":123,"capacity":4,"version":"1.0.0"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void heartbeatWorker_returns204WhenFound() throws Exception {
        UUID workerId = UUID.randomUUID();
        when(workerService.heartbeatWorker(workerId)).thenReturn(true);

        mockMvc.perform(post("/api/v1/workers/" + workerId + "/heartbeat")
                        .header("Authorization", adminToken(UUID.randomUUID())))
                .andExpect(status().isNoContent());
    }

    @Test
    void heartbeatWorker_returns404WhenNotFound() throws Exception {
        UUID workerId = UUID.randomUUID();
        when(workerService.heartbeatWorker(workerId)).thenReturn(false);

        mockMvc.perform(post("/api/v1/workers/" + workerId + "/heartbeat")
                        .header("Authorization", adminToken(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWorkers_withoutQueueTypeParam_usesGetWorkers() throws Exception {
        when(workerService.getWorkers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/workers").header("Authorization", adminToken(UUID.randomUUID())))
                .andExpect(status().isOk());

        verify(workerService).getWorkers();
        verify(workerService, never()).getWorkersByType(any());
    }

    @Test
    void getWorkers_withQueueTypeParam_usesGetWorkersByType() throws Exception {
        when(workerService.getWorkersByType(QueueType.DEFAULT_PRIORITY)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/workers")
                        .header("Authorization", adminToken(UUID.randomUUID()))
                        .param("queueType", "DEFAULT_PRIORITY"))
                .andExpect(status().isOk());

        verify(workerService).getWorkersByType(QueueType.DEFAULT_PRIORITY);
        verify(workerService, never()).getWorkers();
    }

    @Test
    void getTaskExecutionsByWorkerType_returns200() throws Exception {
        TaskExecution execution = new TaskExecution();
        execution.setId(UUID.randomUUID());
        execution.setStatus(TaskExecutionStatus.SUCCESS);
        when(workerService.getTaskExecutionsByWorkerType(QueueType.HIGH_PRIORITY)).thenReturn(List.of(execution));

        mockMvc.perform(get("/api/v1/workers/executions")
                        .header("Authorization", adminToken(UUID.randomUUID()))
                        .param("queueType", "HIGH_PRIORITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    void claimTask_returns200WithClaimedResponse() throws Exception {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        ClaimTaskResponse response = ClaimTaskResponse.claimed(
                executionOf(taskId), taskOf(taskId, TaskType.SIMULATED_WORK));
        when(workerTaskService.claimTask(workerId, taskId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/workers/" + workerId + "/tasks/" + taskId + "/claim")
                        .header("Authorization", adminToken(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("CLAIMED"))
                .andExpect(jsonPath("$.taskType").value("SIMULATED_WORK"));
    }

    @Test
    void claimTask_returns200WithSkippedResponse() throws Exception {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(workerTaskService.claimTask(workerId, taskId)).thenReturn(ClaimTaskResponse.skipped("TASK_CANCELLED"));

        mockMvc.perform(post("/api/v1/workers/" + workerId + "/tasks/" + taskId + "/claim")
                        .header("Authorization", adminToken(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("SKIPPED"))
                .andExpect(jsonPath("$.skipReason").value("TASK_CANCELLED"));
    }

    @Test
    void completeExecution_returns204() throws Exception {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/workers/" + workerId + "/tasks/" + taskId + "/executions/" + executionId + "/complete")
                        .header("Authorization", adminToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"success\":true}"))
                .andExpect(status().isNoContent());

        verify(workerTaskService).completeExecution(workerId, taskId, executionId, true, null, null);
    }

    @Test
    void completeExecution_returns400WhenSuccessFieldMissing() throws Exception {
        mockMvc.perform(post("/api/v1/workers/" + UUID.randomUUID() + "/tasks/" + UUID.randomUUID()
                        + "/executions/" + UUID.randomUUID() + "/complete")
                        .header("Authorization", adminToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private static TaskExecution executionOf(UUID taskId) {
        TaskExecution execution = new TaskExecution();
        execution.setId(UUID.randomUUID());
        execution.setTaskId(taskId);
        execution.setAttemptNumber(1);
        execution.setStartedAt(Instant.now());
        return execution;
    }

    private static com.entity.Task taskOf(UUID taskId, TaskType taskType) {
        com.entity.Task task = new com.entity.Task();
        task.setId(taskId);
        task.setTaskType(taskType);
        return task;
    }
}
