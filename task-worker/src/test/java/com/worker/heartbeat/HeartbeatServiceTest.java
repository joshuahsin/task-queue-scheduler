package com.worker.heartbeat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.worker.client.SchedulerApiClient;
import com.worker.registration.WorkerRegistrationHolder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeartbeatServiceTest {

    @Mock
    private SchedulerApiClient schedulerApiClient;
    @Mock
    private WorkerRegistrationHolder registrationHolder;

    private HeartbeatService heartbeatService;

    @BeforeEach
    void setUp() {
        heartbeatService = new HeartbeatService(schedulerApiClient, registrationHolder);
    }

    @Test
    void heartbeat_sendsHeartbeatForTheRegisteredWorkerId() {
        UUID workerId = UUID.randomUUID();
        when(registrationHolder.requireWorkerId()).thenReturn(workerId);

        heartbeatService.heartbeat();

        verify(schedulerApiClient).heartbeat(workerId);
    }

    @Test
    void heartbeat_swallowsExceptionsFromTheApiCall() {
        UUID workerId = UUID.randomUUID();
        when(registrationHolder.requireWorkerId()).thenReturn(workerId);
        doThrow(new RuntimeException("scheduler api unreachable")).when(schedulerApiClient).heartbeat(workerId);

        assertThatCode(() -> heartbeatService.heartbeat()).doesNotThrowAnyException();
    }

    @Test
    void heartbeat_swallowsExceptionWhenWorkerNotYetRegistered() {
        when(registrationHolder.requireWorkerId()).thenThrow(new IllegalStateException("not registered yet"));

        assertThatCode(() -> heartbeatService.heartbeat()).doesNotThrowAnyException();
    }
}
