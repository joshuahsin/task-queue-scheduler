package com.worker.client.dto;

import com.worker.model.ErrorType;

public record CompleteExecutionRequest(boolean success, ErrorType errorType, String errorMessage) {
}
