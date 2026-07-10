package com.worker.queue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.model.GetMessages;
import com.oracle.bmc.queue.requests.DeleteMessageRequest;
import com.oracle.bmc.queue.requests.GetMessagesRequest;
import com.oracle.bmc.queue.responses.GetMessagesResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerQueueServiceTest {

    private static final String QUEUE_ID = "queue-ocid";

    @Mock
    private QueueClient queueClient;

    private WorkerQueueService workerQueueService;

    @BeforeEach
    void setUp() {
        workerQueueService = new WorkerQueueService(queueClient, QUEUE_ID);
    }

    @Test
    void getMessages_returnsMessagesFromResponseAndPassesThroughParameters() {
        GetMessage message = GetMessage.builder().content("task-content").receipt("receipt-1").build();
        GetMessagesResponse response = GetMessagesResponse.builder()
                .getMessages(GetMessages.builder().messages(List.of(message)).build())
                .build();
        when(queueClient.getMessages(any(GetMessagesRequest.class))).thenReturn(response);

        List<GetMessage> result = workerQueueService.getMessages(5, 60, 10);

        assertThat(result).containsExactly(message);

        ArgumentCaptor<GetMessagesRequest> requestCaptor = ArgumentCaptor.forClass(GetMessagesRequest.class);
        verify(queueClient).getMessages(requestCaptor.capture());
        GetMessagesRequest request = requestCaptor.getValue();
        assertThat(request.getQueueId()).isEqualTo(QUEUE_ID);
        assertThat(request.getLimit()).isEqualTo(5);
        assertThat(request.getVisibilityInSeconds()).isEqualTo(60);
        assertThat(request.getTimeoutInSeconds()).isEqualTo(10);
    }

    @Test
    void getMessages_returnsEmptyListWhenNoMessagesAvailable() {
        GetMessagesResponse response = GetMessagesResponse.builder()
                .getMessages(GetMessages.builder().messages(List.of()).build())
                .build();
        when(queueClient.getMessages(any(GetMessagesRequest.class))).thenReturn(response);

        List<GetMessage> result = workerQueueService.getMessages(10, 60, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteMessage_sendsReceiptForTheConfiguredQueue() {
        workerQueueService.deleteMessage("receipt-to-delete");

        ArgumentCaptor<DeleteMessageRequest> requestCaptor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(queueClient).deleteMessage(requestCaptor.capture());
        DeleteMessageRequest request = requestCaptor.getValue();
        assertThat(request.getQueueId()).isEqualTo(QUEUE_ID);
        assertThat(request.getMessageReceipt()).isEqualTo("receipt-to-delete");
    }
}
