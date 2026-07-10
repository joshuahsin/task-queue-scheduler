package com.worker.registration;

import java.net.InetAddress;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import com.worker.client.SchedulerApiClient;
import com.worker.client.dto.RegisterWorkerRequest;
import com.worker.config.WorkerProperties;
import com.worker.model.QueueType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerBootstrapTest {

    @Mock
    private SchedulerApiClient schedulerApiClient;
    @Mock
    private WorkerRegistrationHolder registrationHolder;
    @Mock
    private ApplicationArguments applicationArguments;

    private WorkerProperties properties;
    private WorkerBootstrap workerBootstrap;

    @BeforeEach
    void setUp() {
        properties = new WorkerProperties();
        properties.setQueueType(QueueType.HIGH_PRIORITY);
        properties.setCapacity(4);
        properties.setVersion("1.0.0");
        workerBootstrap = new WorkerBootstrap(properties, schedulerApiClient, registrationHolder);
    }

    @Test
    void run_registersWithConfiguredNameAndStoresReturnedWorkerId() throws Exception {
        properties.setName("custom-worker");
        UUID workerId = UUID.randomUUID();
        when(schedulerApiClient.register(any())).thenReturn(workerId);

        workerBootstrap.run(applicationArguments);

        ArgumentCaptor<RegisterWorkerRequest> captor = ArgumentCaptor.forClass(RegisterWorkerRequest.class);
        verify(schedulerApiClient).register(captor.capture());
        RegisterWorkerRequest request = captor.getValue();
        assertThat(request.name()).isEqualTo("custom-worker");
        assertThat(request.queueType()).isEqualTo(QueueType.HIGH_PRIORITY);
        assertThat(request.capacity()).isEqualTo(4);
        assertThat(request.version()).isEqualTo("1.0.0");
        assertThat(request.pid()).isEqualTo((int) ProcessHandle.current().pid());

        verify(registrationHolder).setWorkerId(workerId);
    }

    @Test
    void run_defaultsNameToHostnameDashQueueTypeWhenNameIsBlank() throws Exception {
        properties.setName("");
        when(schedulerApiClient.register(any())).thenReturn(UUID.randomUUID());
        String expectedHostname = InetAddress.getLocalHost().getHostName();

        workerBootstrap.run(applicationArguments);

        ArgumentCaptor<RegisterWorkerRequest> captor = ArgumentCaptor.forClass(RegisterWorkerRequest.class);
        verify(schedulerApiClient).register(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo(expectedHostname + "-" + QueueType.HIGH_PRIORITY);
    }

    @Test
    void run_defaultsNameWhenNameIsNull() throws Exception {
        properties.setName(null);
        when(schedulerApiClient.register(any())).thenReturn(UUID.randomUUID());
        String expectedHostname = InetAddress.getLocalHost().getHostName();

        workerBootstrap.run(applicationArguments);

        ArgumentCaptor<RegisterWorkerRequest> captor = ArgumentCaptor.forClass(RegisterWorkerRequest.class);
        verify(schedulerApiClient).register(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo(expectedHostname + "-" + QueueType.HIGH_PRIORITY);
    }

    @Test
    void run_propagatesExceptionWhenRegistrationFails() {
        when(schedulerApiClient.register(any())).thenThrow(new RuntimeException("scheduler api unreachable"));

        assertThatThrownBy(() -> workerBootstrap.run(applicationArguments))
                .isInstanceOf(RuntimeException.class);

        verify(registrationHolder, never()).setWorkerId(any());
    }
}
