package com.worker.poll;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.oracle.bmc.queue.model.GetMessage;
import com.worker.client.SchedulerApiClient;
import com.worker.client.dto.ClaimTaskResponse;
import com.worker.config.WorkerProperties;
import com.worker.exec.TaskRunner;
import com.worker.model.ClaimOutcome;
import com.worker.model.ErrorType;
import com.worker.queue.WorkerQueueService;
import com.worker.registration.WorkerRegistrationHolder;

// Polls this worker's assigned OCI queue and dispatches each message onto a fixed-size executor.
//
// Polling is gated by a Semaphore(capacity), not just a fixed-size ExecutorService's own internal
// queue: claiming a message (via SchedulerApiClient.claim) already flips the Task to RUNNING and
// creates a TaskExecution row on the scheduler API before actual work starts. Letting the poller
// claim more messages than can immediately run would leave tasks "claimed but not yet started"
// sitting in the executor's queue, which both violates Worker.capacity's stated meaning ("max
// concurrent tasks this worker's thread pool can run") and skews the scheduler API's queue-wait
// metrics. The semaphore keeps true in-flight claimed executions bounded to capacity.
@Service
public class TaskPollingService {
    private static final Logger log = LoggerFactory.getLogger(TaskPollingService.class);
    private static final int MAX_MESSAGES_PER_POLL = 10;

    private final WorkerQueueService queueService;
    private final SchedulerApiClient schedulerApiClient;
    private final WorkerRegistrationHolder registrationHolder;
    private final TaskRunner taskRunner;
    private final WorkerProperties properties;

    private final Semaphore permits;
    private final ExecutorService executor;

    public TaskPollingService(WorkerQueueService queueService, SchedulerApiClient schedulerApiClient,
            WorkerRegistrationHolder registrationHolder, TaskRunner taskRunner, WorkerProperties properties) {
        this.queueService = queueService;
        this.schedulerApiClient = schedulerApiClient;
        this.registrationHolder = registrationHolder;
        this.taskRunner = taskRunner;
        this.properties = properties;
        this.permits = new Semaphore(properties.getCapacity());
        this.executor = Executors.newFixedThreadPool(properties.getCapacity());
    }

    @Scheduled(fixedDelayString = "${worker.poll-delay-ms:1000}")
    public void pollAndDispatch() {
        int available = permits.availablePermits();
        if (available == 0) {
            return;
        }

        List<GetMessage> messages = queueService.getMessages(Math.min(available, MAX_MESSAGES_PER_POLL),
                properties.getVisibilityTimeoutSeconds(), properties.getLongPollTimeoutSeconds());

        for (GetMessage message : messages) {
            permits.acquireUninterruptibly();
            executor.submit(() -> handle(message));
        }
    }

    private void handle(GetMessage message) {
        try {
            UUID taskId;
            try {
                taskId = UUID.fromString(message.getContent());
            } catch (IllegalArgumentException e) {
                log.warn("Discarding malformed queue message, content was not a UUID: {}", message.getContent());
                queueService.deleteMessage(message.getReceipt());
                return;
            }

            UUID workerId = registrationHolder.requireWorkerId();
            ClaimTaskResponse claim = schedulerApiClient.claim(workerId, taskId);
            if (claim.outcome() == ClaimOutcome.SKIPPED) {
                log.info("Skipping task {}: {}", taskId, claim.skipReason());
                queueService.deleteMessage(message.getReceipt());
                return;
            }

            boolean success;
            ErrorType errorType = null;
            String errorMessage = null;
            try {
                success = switch (claim.taskType()) {
                    case SIMULATED_WORK -> taskRunner.simulateWork();
                    case SEND_EMAIL -> taskRunner.simulateSendEmail(claim.payload());
                };
            } catch (Exception e) {
                success = false;
                errorType = ErrorType.TRANSIENT;
                errorMessage = e.getMessage();
            }

            schedulerApiClient.complete(workerId, taskId, claim.executionId(), success, errorType, errorMessage);

            if (success) {
                // On failure the message is deliberately left in the queue — it becomes visible
                // again once the visibility timeout expires, so another poll picks it up.
                queueService.deleteMessage(message.getReceipt());
            }
        } catch (Exception e) {
            log.error("Failed to process queue message", e);
        } finally {
            permits.release();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
