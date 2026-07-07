package com.worker.registration;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.worker.client.SchedulerApiClient;
import com.worker.client.dto.RegisterWorkerRequest;
import com.worker.config.WorkerProperties;

// Registers this process with the scheduler API once at startup. Deliberately fails fast (lets
// the exception propagate and startup fail) if the scheduler API is unreachable rather than
// starting up half-alive with no worker id — an orchestrator should restart the process with
// backoff instead.
@Component
public class WorkerBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(WorkerBootstrap.class);

    private final WorkerProperties properties;
    private final SchedulerApiClient schedulerApiClient;
    private final WorkerRegistrationHolder registrationHolder;

    public WorkerBootstrap(WorkerProperties properties, SchedulerApiClient schedulerApiClient,
            WorkerRegistrationHolder registrationHolder) {
        this.properties = properties;
        this.schedulerApiClient = schedulerApiClient;
        this.registrationHolder = registrationHolder;
    }

    @Override
    public void run(ApplicationArguments args) throws UnknownHostException {
        String hostname = InetAddress.getLocalHost().getHostName();
        int pid = (int) ProcessHandle.current().pid();
        String name = properties.getName() != null && !properties.getName().isBlank()
                ? properties.getName()
                : hostname + "-" + properties.getQueueType();

        var request = new RegisterWorkerRequest(name, properties.getQueueType(), hostname, pid,
                properties.getCapacity(), properties.getVersion());
        var workerId = schedulerApiClient.register(request);
        registrationHolder.setWorkerId(workerId);

        log.info("Registered as worker {} ({}) for queue {}", workerId, name, properties.getQueueType());
    }
}
