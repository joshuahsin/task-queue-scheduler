package com.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.entity.APIKey;
import com.enums.Enums.ApiKeyStatus;

public interface APIKeyRepo extends JpaRepository<APIKey, UUID> {
    Optional<APIKey> findByKeyHash(String keyHash);
    Optional<APIKey> findById(UUID id);
    List<APIKey> findByTenantId(UUID tenantId);
    List<APIKey> findByTenantIdAndStatus(UUID tenantId, ApiKeyStatus status);
}
