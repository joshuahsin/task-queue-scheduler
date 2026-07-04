package com.DTO;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.enums.Enums.PriorityType;
import com.enums.Enums.TaskType;

@Getter
@NoArgsConstructor
public class QueueTaskRequest {
    @NotNull
    private TaskType type;
    @NotNull
    private PriorityType priority;
    private Instant scheduledAt;
}
