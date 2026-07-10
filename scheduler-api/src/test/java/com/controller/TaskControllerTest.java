package com.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;

import com.entity.Task;
import com.entity.TaskExecution;
import com.enums.Enums.PriorityType;
import com.enums.Enums.TaskStatus;
import com.enums.Enums.TaskType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TaskController.class)
class TaskControllerTest extends AbstractControllerTest {

    @Test
    void queueTask_returnsCreatedTaskId() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(taskService.queueTask(any(), any(), any(), any())).thenReturn(taskId);

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", adminToken(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SIMULATED_WORK\",\"priority\":\"HIGH_PRIORITY\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("\"" + taskId + "\""));

        verify(taskService).queueTask(eq(tenantId), eq(TaskType.SIMULATED_WORK), eq(PriorityType.HIGH_PRIORITY), any());
    }

    @Test
    void queueTask_returns400WhenTypeOrPriorityMissing() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", adminToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTask_returns200WhenTaskBelongsToCaller() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTenantId(tenantId);
        task.setTaskType(TaskType.SEND_EMAIL);
        when(taskService.getTask(taskId, tenantId)).thenReturn(Optional.of(task));

        mockMvc.perform(get("/api/v1/tasks/" + taskId).header("Authorization", adminToken(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.taskType").value("SEND_EMAIL"));
    }

    @Test
    void getTask_returns404WhenNotFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(taskService.getTask(taskId, tenantId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tasks/" + taskId).header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelTask_returns204WhenCancelled() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(taskService.cancelTask(taskId, tenantId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/tasks/" + taskId).header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelTask_returns404WhenNotFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(taskService.cancelTask(taskId, tenantId)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/tasks/" + taskId).header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTasksQuery_defaultsToFirstPageOfTwentyWithNoFilters() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(taskService.getTasksQuery(eq(tenantId), eq(-1), eq(null), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/tasks").header("Authorization", adminToken(tenantId)))
                .andExpect(status().isOk());

        verify(taskService).getTasksQuery(tenantId, -1, null, 0, 20);
    }

    @Test
    void getTasksQuery_passesThroughGivenPriorityStatusAndPaging() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(taskService.getTasksQuery(any(), any(Integer.class), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", adminToken(tenantId))
                        .param("priority", "DEFAULT_PRIORITY")
                        .param("status", "RETRYING")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(taskService).getTasksQuery(tenantId, PriorityType.DEFAULT_PRIORITY.ordinal(), TaskStatus.RETRYING, 2, 5);
    }

    @Test
    void getTaskExecutions_returns404WhenTaskNotOwnedByCaller() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(taskService.getTask(taskId, tenantId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/executions").header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTaskExecutions_returns200WithExecutionsWhenTaskFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTenantId(tenantId);
        when(taskService.getTask(taskId, tenantId)).thenReturn(Optional.of(task));
        TaskExecution execution = new TaskExecution();
        execution.setId(UUID.randomUUID());
        execution.setTaskId(taskId);
        when(taskExecutionService.getTaskExecutions(taskId)).thenReturn(List.of(execution));

        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/executions").header("Authorization", adminToken(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(taskId.toString()));
    }

    @Test
    void cancelTaskExecution_returns404WhenTaskNotOwnedByCaller() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(taskService.getTask(taskId, tenantId)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/tasks/" + taskId + "/executions").header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelTaskExecution_returns404WhenExecutionNotFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTenantId(tenantId);
        when(taskService.getTask(taskId, tenantId)).thenReturn(Optional.of(task));
        when(taskExecutionService.cancelTaskExecution(taskId)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/tasks/" + taskId + "/executions").header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelTaskExecution_returns204WhenCancelled() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTenantId(tenantId);
        when(taskService.getTask(taskId, tenantId)).thenReturn(Optional.of(task));
        when(taskExecutionService.cancelTaskExecution(taskId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/tasks/" + taskId + "/executions").header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNoContent());
    }
}
