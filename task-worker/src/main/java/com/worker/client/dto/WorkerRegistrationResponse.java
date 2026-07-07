package com.worker.client.dto;

import java.util.UUID;

// Deliberately minimal — the scheduler-api's WorkerResponse has several more fields, but Jackson's
// default lenient deserialization ignores JSON properties this record doesn't declare, and the
// worker only ever needs its own assigned id back.
public record WorkerRegistrationResponse(UUID id) {
}
