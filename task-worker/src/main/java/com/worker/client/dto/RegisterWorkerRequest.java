package com.worker.client.dto;

import com.worker.model.QueueType;

public record RegisterWorkerRequest(String name, QueueType queueType, String hostname, int pid, int capacity, String version) {
}
