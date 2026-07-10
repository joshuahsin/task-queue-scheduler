package com.worker.poll;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.oracle.bmc.queue.model.GetMessage;
import com.worker.client.SchedulerApiClient;
import com.worker.client.dto.ClaimTaskResponse;
import com.worker.config.WorkerProperties;
import com.worker.exec.TaskRunner;
import com.worker.model.ClaimOutcome;
import com.worker.model.ErrorType;
import com.worker.model.TaskType;
import com.worker.queue.WorkerQueueService;
import com.worker.registration.WorkerRegistrationHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskPollingServiceTest {

    private static final long ASYNC_TIMEOUT_MS = 2000;

    @Mock
    private WorkerQueueService queueService;
    @Mock
    private SchedulerApiClient schedulerApiClient;
    @Mock
    private WorkerRegistrationHolder registrationHolder;
    @Mock
    private TaskRunner taskRunner;

    private TaskPollingService taskPollingService;

    @BeforeEach
    void setUp() {
        WorkerProperties properties = new WorkerProperties();
        properties.setCapacity(4);
        properties.setVisibilityTimeoutSeconds(60);
        properties.setLongPollTimeoutSeconds(5);
        taskPollingService = new TaskPollingService(queueService, schedulerApiClient, registrationHolder, taskRunner, properties);
    }

    @AfterEach
    void tearDown() {
        taskPollingService.shutdown();
    }

    private static GetMessage message(String content, String receipt) {
        return GetMessage.builder().content(content).receipt(receipt).build();
    }

    @Test
    void pollAndDispatch_requestsUpToAvailablePermitsWithConfiguredTimeouts() {
        when(queueService.getMessages(anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        taskPollingService.pollAndDispatch();

        verify(queueService).getMessages(4, 60, 5);
    }

    @Test
    void pollAndDispatch_malformedUuidContent_deletesMessageWithoutClaiming() {
        when(queueService.getMessages(anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(message("not-a-uuid", "r1")));

        taskPollingService.pollAndDispatch();

        verify(queueService, timeout(ASYNC_TIMEOUT_MS)).deleteMessage("r1");
        verify(schedulerApiClient, never()).claim(any(), any());
    }

    @Test
    void pollAndDispatch_skippedClaim_deletesMessageWithoutRunningTask() {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(queueService.getMessages(anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(message(taskId.toString(), "r2")));
        when(registrationHolder.requireWorkerId()).thenReturn(workerId);
        when(schedulerApiClient.claim(workerId, taskId))
                .thenReturn(new ClaimTaskResponse(ClaimOutcome.SKIPPED, null, null, null, null, "TASK_CANCELLED"));

        taskPollingService.pollAndDispatch();

        verify(queueService, timeout(ASYNC_TIMEOUT_MS)).deleteMessage("r2");
        verify(taskRunner, never()).simulateWork();
        verify(taskRunner, never()).simulateSendEmail(any());
    }

    @Test
    void pollAndDispatch_claimedSimulatedWorkSucceeds_completesSuccessfullyAndDeletesMessage() {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        when(queueService.getMessages(anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(message(taskId.toString(), "r3")));
        when(registrationHolder.requireWorkerId()).thenReturn(workerId);
        when(schedulerApiClient.claim(workerId, taskId))
                .thenReturn(new ClaimTaskResponse(ClaimOutcome.CLAIMED, executionId, 1, TaskType.SIMULATED_WORK, null, null));
        when(taskRunner.simulateWork()).thenReturn(true);

        taskPollingService.pollAndDispatch();

        verify(schedulerApiClient, timeout(ASYNC_TIMEOUT_MS)).complete(workerId, taskId, executionId, true, null, null);
        verify(queueService).deleteMessage("r3");
    }

    @Test
    void pollAndDispatch_claimedTaskReturnsFailure_completesWithFailureAndLeavesMessageInQueue() {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        when(queueService.getMessages(anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(message(taskId.toString(), "r4")));
        when(registrationHolder.requireWorkerId()).thenReturn(workerId);
        when(schedulerApiClient.claim(workerId, taskId))
                .thenReturn(new ClaimTaskResponse(ClaimOutcome.CLAIMED, executionId, 1, TaskType.SIMULATED_WORK, null, null));
        when(taskRunner.simulateWork()).thenReturn(false);

        taskPollingService.pollAndDispatch();

        verify(schedulerApiClient, timeout(ASYNC_TIMEOUT_MS)).complete(workerId, taskId, executionId, false, null, null);
        verify(queueService, never()).deleteMessage(any());
    }

    @Test
    void pollAndDispatch_claimedSendEmail_passesPayloadToTaskRunner() {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("to", "a@b.com");
        when(queueService.getMessages(anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(message(taskId.toString(), "r5")));
        when(registrationHolder.requireWorkerId()).thenReturn(workerId);
        when(schedulerApiClient.claim(workerId, taskId))
                .thenReturn(new ClaimTaskResponse(ClaimOutcome.CLAIMED, executionId, 1, TaskType.SEND_EMAIL, payload, null));
        when(taskRunner.simulateSendEmail(payload)).thenReturn(true);

        taskPollingService.pollAndDispatch();

        verify(taskRunner, timeout(ASYNC_TIMEOUT_MS)).simulateSendEmail(payload);
        verify(schedulerApiClient).complete(workerId, taskId, executionId, true, null, null);
        verify(taskRunner, never()).simulateWork();
    }

    @Test
    void pollAndDispatch_taskRunnerThrows_reportsTransientFailureAndLeavesMessageInQueue() {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        when(queueService.getMessages(anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(message(taskId.toString(), "r6")));
        when(registrationHolder.requireWorkerId()).thenReturn(workerId);
        when(schedulerApiClient.claim(workerId, taskId))
                .thenReturn(new ClaimTaskResponse(ClaimOutcome.CLAIMED, executionId, 1, TaskType.SIMULATED_WORK, null, null));
        when(taskRunner.simulateWork()).thenThrow(new RuntimeException("boom"));

        taskPollingService.pollAndDispatch();

        verify(schedulerApiClient, timeout(ASYNC_TIMEOUT_MS)).complete(workerId, taskId, executionId, false, ErrorType.TRANSIENT, "boom");
        verify(queueService, never()).deleteMessage(any());
    }
}
