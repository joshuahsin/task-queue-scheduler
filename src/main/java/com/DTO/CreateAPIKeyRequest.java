package com.DTO;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateAPIKeyRequest {
    @NotNull
    private UUID tenantId;
    @NotBlank
    private String name;
    private Instant expiresAt;
}
