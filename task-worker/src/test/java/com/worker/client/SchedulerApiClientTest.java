package com.worker.client;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.worker.client.dto.ClaimTaskResponse;
import com.worker.client.dto.RegisterWorkerRequest;
import com.worker.model.ClaimOutcome;
import com.worker.model.ErrorType;
import com.worker.model.QueueType;
import com.worker.model.TaskType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SchedulerApiClientTest {

    private static final String BASE_URL = "http://scheduler-api.test";

    private MockRestServiceServer mockServer;
    private SchedulerApiClient schedulerApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        schedulerApiClient = new SchedulerApiClient(builder.build());
    }

    @Test
    void register_postsToWorkersEndpointAndReturnsId() {
        UUID workerId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/v1/workers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.name").value("worker-1"))
                .andExpect(jsonPath("$.queueType").value("HIGH_PRIORITY"))
                .andRespond(withSuccess("{\"id\":\"" + workerId + "\"}", MediaType.APPLICATION_JSON));

        UUID result = schedulerApiClient.register(
                new RegisterWorkerRequest("worker-1", QueueType.HIGH_PRIORITY, "host-1", 123, 4, "1.0.0"));

        assertThat(result).isEqualTo(workerId);
        mockServer.verify();
    }

    @Test
    void heartbeat_postsToHeartbeatEndpointForGivenWorker() {
        UUID workerId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/v1/workers/" + workerId + "/heartbeat"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withNoContent());

        schedulerApiClient.heartbeat(workerId);

        mockServer.verify();
    }

    @Test
    void claim_postsToClaimEndpointAndReturnsParsedResponse() {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/v1/workers/" + workerId + "/tasks/" + taskId + "/claim"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"outcome":"CLAIMED","executionId":"%s","attemptNumber":1,"taskType":"SIMULATED_WORK"}
                        """.formatted(executionId), MediaType.APPLICATION_JSON));

        ClaimTaskResponse response = schedulerApiClient.claim(workerId, taskId);

        assertThat(response.outcome()).isEqualTo(ClaimOutcome.CLAIMED);
        assertThat(response.executionId()).isEqualTo(executionId);
        assertThat(response.attemptNumber()).isEqualTo(1);
        assertThat(response.taskType()).isEqualTo(TaskType.SIMULATED_WORK);
        mockServer.verify();
    }

    @Test
    void claim_parsesSkippedResponse() {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/v1/workers/" + workerId + "/tasks/" + taskId + "/claim"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"outcome\":\"SKIPPED\",\"skipReason\":\"TASK_CANCELLED\"}", MediaType.APPLICATION_JSON));

        ClaimTaskResponse response = schedulerApiClient.claim(workerId, taskId);

        assertThat(response.outcome()).isEqualTo(ClaimOutcome.SKIPPED);
        assertThat(response.skipReason()).isEqualTo("TASK_CANCELLED");
    }

    @Test
    void complete_postsCompletionOutcomeToTheRightExecution() {
        UUID workerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/v1/workers/" + workerId + "/tasks/" + taskId
                        + "/executions/" + executionId + "/complete"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorType").value("TRANSIENT"))
                .andExpect(jsonPath("$.errorMessage").value("boom"))
                .andRespond(withNoContent());

        schedulerApiClient.complete(workerId, taskId, executionId, false, ErrorType.TRANSIENT, "boom");

        mockServer.verify();
    }
}
