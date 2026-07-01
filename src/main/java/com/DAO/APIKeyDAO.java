package com.DAO;

import java.time.Instant;
import java.util.UUID;

import com.enums.Enums.ApiKeyStatus;

public class APIKeyDAO {
    private UUID id;
    private UUID tenantId;
    private String name;
    private String keyHash;
    private ApiKeyStatus status;
    private Instant createdAt;
    private Instant expiresAt;
}
