package com.worker.client;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.worker.client.dto.ClaimTaskResponse;
import com.worker.client.dto.CompleteExecutionRequest;
import com.worker.client.dto.RegisterWorkerRequest;
import com.worker.client.dto.WorkerRegistrationResponse;
import com.worker.model.ErrorType;

@Component
public class SchedulerApiClient {
    private final RestClient restClient;

    public SchedulerApiClient(RestClient schedulerApiRestClient) {
        this.restClient = schedulerApiRestClient;
    }

    public UUID register(RegisterWorkerRequest request) {
        WorkerRegistrationResponse response = restClient.post()
                .uri("/api/v1/workers")
                .body(request)
                .retrieve()
                .body(WorkerRegistrationResponse.class);
        return response.id();
    }

    public void heartbeat(UUID workerId) {
        restClient.post()
                .uri("/api/v1/workers/{workerId}/heartbeat", workerId)
                .retrieve()
                .toBodilessEntity();
    }

    public void deregister(UUID workerId) {
        restClient.delete()
                .uri("/api/v1/workers/{workerId}", workerId)
                .retrieve()
                .toBodilessEntity();
    }

    public ClaimTaskResponse claim(UUID workerId, UUID taskId) {
        return restClient.post()
                .uri("/api/v1/workers/{workerId}/tasks/{taskId}/claim", workerId, taskId)
                .retrieve()
                .body(ClaimTaskResponse.class);
    }

    public void complete(UUID workerId, UUID taskId, UUID executionId, boolean success, ErrorType errorType, String errorMessage) {
        restClient.post()
                .uri("/api/v1/workers/{workerId}/tasks/{taskId}/executions/{executionId}/complete", workerId, taskId, executionId)
                .body(new CompleteExecutionRequest(success, errorType, errorMessage))
                .retrieve()
                .toBodilessEntity();
    }
}
