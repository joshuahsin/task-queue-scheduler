package com.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.enums.Enums.ErrorType;

@Getter
@NoArgsConstructor
public class CompleteExecutionRequest {
    @NotNull
    private Boolean success;
    private ErrorType errorType;
    private String errorMessage;
}
