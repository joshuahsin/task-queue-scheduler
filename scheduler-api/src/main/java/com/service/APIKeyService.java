package com.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.DAO.APIKeyDAO;
import com.entity.APIKey;
import com.enums.Enums.ApiKeyStatus;
import com.exception.InvalidApiKeyException;
import com.repo.APIKeyRepo;
import com.security.JwtUtil;

@Service
public class APIKeyService implements APIKeyDAO {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final APIKeyRepo apiKeyRepo;
    private final JwtUtil jwtUtil;

    public APIKeyService(APIKeyRepo apiKeyRepo, JwtUtil jwtUtil) {
        this.apiKeyRepo = apiKeyRepo;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String createAPIKey(UUID tenantId, String name, Instant expiresAt) {
        String rawKey = generateRawKey();

        APIKey apiKey = new APIKey();
        apiKey.setTenantId(tenantId);
        apiKey.setName(name);
        apiKey.setKeyHash(hash(rawKey));
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        apiKey.setCreatedAt(Instant.now());
        apiKey.setExpiresAt(expiresAt);

        apiKeyRepo.save(apiKey);
        return rawKey;
    }

    @Override
    public String getToken(String apiKey) {
        APIKey record = apiKeyRepo.findByKeyHash(hash(apiKey))
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API key"));

        if (record.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new InvalidApiKeyException("API key is inactive");
        }
        if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidApiKeyException("API key has expired");
        }

        return jwtUtil.generateToken(record.getTenantId());
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "sk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    public boolean revokeAPIKey(UUID apiKeyId, UUID tenantId) {
        return apiKeyRepo.findById(apiKeyId)
                .filter(apiKey -> apiKey.getTenantId().equals(tenantId))
                .map(apiKey -> {
                    apiKey.setStatus(ApiKeyStatus.INACTIVE);
                    apiKeyRepo.save(apiKey);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public List<APIKey> getAPIKeys(UUID tenantId, ApiKeyStatus status) {
        return status == null
                ? apiKeyRepo.findByTenantId(tenantId)
                : apiKeyRepo.findByTenantIdAndStatus(tenantId, status);
    }
}
