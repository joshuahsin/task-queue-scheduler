package com.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.enums.Enums.QueueType;

@Getter
@NoArgsConstructor
public class RegisterWorkerRequest {
    @NotBlank
    private String name;
    @NotNull
    private QueueType queueType;
    @NotBlank
    private String hostname;
    private int pid;
    private int capacity;
    private String version;
}
