package com.worker.queue;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.queue.QueueAdminClient;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.requests.DeleteMessageRequest;
import com.oracle.bmc.queue.requests.GetMessagesRequest;
import com.oracle.bmc.queue.requests.GetQueueRequest;

// Single-queue counterpart to the scheduler-api's QueueConnectionManager: a worker process is
// assigned exactly one QueueType (see worker.queue-type), so there's no need for the EnumMap of
// clients the API side holds for both queues at once.
@Component
public class WorkerQueueService {
    private final QueueClient queueClient;
    private final String queueId;

    public WorkerQueueService(BasicAuthenticationDetailsProvider authProvider,
            @Value("${oci.region}") String region,
            @Value("${oci.queue.ocid}") String queueOcid) {

        QueueAdminClient adminClient = QueueAdminClient.builder()
                .region(region)
                .build(authProvider);

        String messagesEndpoint = adminClient.getQueue(GetQueueRequest.builder()
                        .queueId(queueOcid)
                        .build())
                .getQueue()
                .getMessagesEndpoint();

        this.queueClient = QueueClient.builder()
                .endpoint(messagesEndpoint)
                .build(authProvider);
        this.queueId = queueOcid;
    }

    // Package-private: lets tests inject a mock QueueClient directly, bypassing the real OCI admin
    // lookup the public constructor performs (which needs live OCI credentials/network access).
    WorkerQueueService(QueueClient queueClient, String queueId) {
        this.queueClient = queueClient;
        this.queueId = queueId;
    }

    public List<GetMessage> getMessages(int limit, int visibilityTimeoutSeconds, int longPollTimeoutSeconds) {
        return queueClient.getMessages(GetMessagesRequest.builder()
                        .queueId(queueId)
                        .visibilityInSeconds(visibilityTimeoutSeconds)
                        .timeoutInSeconds(longPollTimeoutSeconds)
                        .limit(limit)
                        .build())
                .getGetMessages()
                .getMessages();
    }

    public void deleteMessage(String receiptHandle) {
        queueClient.deleteMessage(DeleteMessageRequest.builder()
                .queueId(queueId)
                .messageReceipt(receiptHandle)
                .build());
    }
}
