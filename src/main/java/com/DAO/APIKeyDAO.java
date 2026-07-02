package com.DAO;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.entity.APIKey;
import com.enums.Enums.ApiKeyStatus;

public interface APIKeyDAO {
    public String getToken(String apiKey);
    public String createAPIKey(UUID tenantID, String name, Instant expiresAt);
    public boolean revokeAPIKey(UUID apiKeyId);
    public List<APIKey> getAPIKeys(UUID tenantId, ApiKeyStatus status);
}
