package com.DAO;

import java.util.UUID;

import com.DTO.ClaimTaskResponse;
import com.enums.Enums.ErrorType;

public interface WorkerTaskDAO {
    ClaimTaskResponse claimTask(UUID workerId, UUID taskId);
    void completeExecution(UUID workerId, UUID taskId, UUID executionId, boolean success, ErrorType errorType, String errorMessage);
}
