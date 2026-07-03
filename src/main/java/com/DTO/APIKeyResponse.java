package com.DTO;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.entity.APIKey;
import com.enums.Enums.ApiKeyStatus;

@Getter
@AllArgsConstructor
public class APIKeyResponse {
    private UUID id;
    private UUID tenantId;
    private String name;
    private ApiKeyStatus status;
    private Instant createdAt;
    private Instant expiresAt;

    public static APIKeyResponse from(APIKey apiKey) {
        return new APIKeyResponse(
            apiKey.getId(), apiKey.getTenantId(), apiKey.getName(),
            apiKey.getStatus(), apiKey.getCreatedAt(), apiKey.getExpiresAt()
        );
    }
}
