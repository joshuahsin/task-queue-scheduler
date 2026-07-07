package com.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.worker.model.QueueType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {
    private String name;
    private QueueType queueType;
    private int capacity = 4;
    private String version = "0.0.1";
    private String schedulerApiBaseUrl;
    private long heartbeatIntervalMs = 30000;
    private long pollDelayMs = 1000;
    private int visibilityTimeoutSeconds = 60;
    private int longPollTimeoutSeconds = 5;
}
