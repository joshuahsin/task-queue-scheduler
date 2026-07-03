package com.DTO;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateAPIKeyRequest {
    @NotBlank
    private String name;
    private Instant expiresAt;
}
