package com.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import com.enums.Enums.ApiKeyStatus;

@Entity
@Table(name = "api_keys")
class ApiKey {
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    private String name;
    private String keyHash;
    private ApiKeyStatus status;
    private Instant createdAt;
    private Instant expiresAt;
}