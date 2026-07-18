package com.worker.registration;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.worker.client.SchedulerApiClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerShutdownHandlerTest {

    @Mock
    private SchedulerApiClient schedulerApiClient;
    @Mock
    private WorkerRegistrationHolder registrationHolder;

    private WorkerShutdownHandler workerShutdownHandler;

    @BeforeEach
    void setUp() {
        workerShutdownHandler = new WorkerShutdownHandler(schedulerApiClient, registrationHolder);
    }

    @Test
    void deregister_deregistersTheRegisteredWorkerId() {
        UUID workerId = UUID.randomUUID();
        when(registrationHolder.requireWorkerId()).thenReturn(workerId);

        workerShutdownHandler.deregister();

        verify(schedulerApiClient).deregister(workerId);
    }

    @Test
    void deregister_swallowsExceptionsFromTheApiCall() {
        UUID workerId = UUID.randomUUID();
        when(registrationHolder.requireWorkerId()).thenReturn(workerId);
        doThrow(new RuntimeException("scheduler api unreachable")).when(schedulerApiClient).deregister(workerId);

        assertThatCode(() -> workerShutdownHandler.deregister()).doesNotThrowAnyException();
    }

    @Test
    void deregister_swallowsExceptionWhenWorkerNeverFinishedRegistering() {
        when(registrationHolder.requireWorkerId()).thenThrow(new IllegalStateException("not registered yet"));

        assertThatCode(() -> workerShutdownHandler.deregister()).doesNotThrowAnyException();

        verify(schedulerApiClient, never()).deregister(any());
    }
}
