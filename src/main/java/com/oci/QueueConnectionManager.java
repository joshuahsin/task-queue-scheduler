package com.oci;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.queue.QueueAdminClient;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.requests.GetQueueRequest;

import com.enums.Enums.QueueType;

// Resolves each configured OCI Queue's per-queue messages endpoint at startup and holds one
// QueueClient per QueueType, ready for publish/consume code to use.
//
// DEAD_LETTER_QUEUE is intentionally not registered here: OCI Queue provisions a dead-letter
// queue implicitly per parent queue rather than as its own top-level queue with an OCID, so
// there's nothing to configure a connection to under that QueueType.
@Component
public class QueueConnectionManager {
    private final Map<QueueType, String> queueIds = new EnumMap<>(QueueType.class);
    private final Map<QueueType, QueueClient> queueClients = new EnumMap<>(QueueType.class);

    public QueueConnectionManager(BasicAuthenticationDetailsProvider authProvider,
            @Value("${oci.region}") String region,
            @Value("${oci.queue.high-priority-ocid}") String highPriorityOcid,
            @Value("${oci.queue.default-priority-ocid}") String defaultPriorityOcid) {

        QueueAdminClient adminClient = QueueAdminClient.builder()
                .region(region)
                .build(authProvider);

        registerQueue(adminClient, authProvider, QueueType.HIGH_PRIORITY, highPriorityOcid);
        registerQueue(adminClient, authProvider, QueueType.DEFAULT_PRIORITY, defaultPriorityOcid);
    }

    private void registerQueue(QueueAdminClient adminClient, BasicAuthenticationDetailsProvider authProvider,
            QueueType type, String queueId) {
        if (queueId == null || queueId.isBlank()) {
            return;
        }

        String messagesEndpoint = adminClient.getQueue(GetQueueRequest.builder()
                        .queueId(queueId)
                        .build())
                .getQueue()
                .getMessagesEndpoint();

        QueueClient client = QueueClient.builder()
                .endpoint(messagesEndpoint)
                .build(authProvider);

        queueIds.put(type, queueId);
        queueClients.put(type, client);
    }

    public QueueClient getClient(QueueType type) {
        QueueClient client = queueClients.get(type);
        if (client == null) {
            throw new IllegalStateException("No queue configured for type: " + type);
        }
        return client;
    }

    public String getQueueId(QueueType type) {
        String queueId = queueIds.get(type);
        if (queueId == null) {
            throw new IllegalStateException("No queue configured for type: " + type);
        }
        return queueId;
    }
}
