package com.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.entity.APIKey;
import com.enums.Enums.ApiKeyStatus;
import com.exception.InvalidApiKeyException;
import com.repo.APIKeyRepo;
import com.security.JwtUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class APIKeyServiceTest {

    @Mock
    private APIKeyRepo apiKeyRepo;
    @Mock
    private JwtUtil jwtUtil;

    private APIKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new APIKeyService(apiKeyRepo, jwtUtil);
    }

    @Test
    void createAPIKey_returnsRawKeyButPersistsOnlyItsHash() {
        UUID tenantId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);
        when(apiKeyRepo.save(any(APIKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String rawKey = apiKeyService.createAPIKey(tenantId, "ci-key", expiresAt);

        assertThat(rawKey).startsWith("sk_");

        ArgumentCaptor<APIKey> captor = ArgumentCaptor.forClass(APIKey.class);
        verify(apiKeyRepo).save(captor.capture());
        APIKey saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getName()).isEqualTo("ci-key");
        assertThat(saved.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(saved.getCreatedAt()).isNotNull();
        // the raw key is never stored verbatim
        assertThat(saved.getKeyHash()).isNotEqualTo(rawKey);
        assertThat(saved.getKeyHash()).matches("[0-9a-f]{64}");
    }

    @Test
    void getToken_returnsJwtForActiveUnexpiredKey() {
        UUID tenantId = UUID.randomUUID();
        APIKey record = new APIKey();
        record.setTenantId(tenantId);
        record.setStatus(ApiKeyStatus.ACTIVE);
        record.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(apiKeyRepo.findByKeyHash(anyString())).thenReturn(Optional.of(record));
        when(jwtUtil.generateToken(tenantId)).thenReturn("jwt-token");

        String token = apiKeyService.getToken("sk_whatever");

        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void getToken_allowsKeyWithNoExpiry() {
        UUID tenantId = UUID.randomUUID();
        APIKey record = new APIKey();
        record.setTenantId(tenantId);
        record.setStatus(ApiKeyStatus.ACTIVE);
        record.setExpiresAt(null);
        when(apiKeyRepo.findByKeyHash(anyString())).thenReturn(Optional.of(record));
        when(jwtUtil.generateToken(tenantId)).thenReturn("jwt-token");

        assertThat(apiKeyService.getToken("sk_whatever")).isEqualTo("jwt-token");
    }

    @Test
    void getToken_throwsWhenKeyDoesNotExist() {
        when(apiKeyRepo.findByKeyHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.getToken("sk_bogus"))
                .isInstanceOf(InvalidApiKeyException.class);
    }

    @Test
    void getToken_throwsWhenKeyIsInactive() {
        APIKey record = new APIKey();
        record.setStatus(ApiKeyStatus.INACTIVE);
        when(apiKeyRepo.findByKeyHash(anyString())).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> apiKeyService.getToken("sk_whatever"))
                .isInstanceOf(InvalidApiKeyException.class);
    }

    @Test
    void getToken_throwsWhenKeyIsExpired() {
        APIKey record = new APIKey();
        record.setStatus(ApiKeyStatus.ACTIVE);
        record.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(apiKeyRepo.findByKeyHash(anyString())).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> apiKeyService.getToken("sk_whatever"))
                .isInstanceOf(InvalidApiKeyException.class);
    }

    @Test
    void revokeAPIKey_setsInactiveWhenTenantMatches() {
        UUID tenantId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        APIKey record = new APIKey();
        record.setId(keyId);
        record.setTenantId(tenantId);
        record.setStatus(ApiKeyStatus.ACTIVE);
        when(apiKeyRepo.findById(keyId)).thenReturn(Optional.of(record));

        boolean result = apiKeyService.revokeAPIKey(keyId, tenantId);

        assertThat(result).isTrue();
        assertThat(record.getStatus()).isEqualTo(ApiKeyStatus.INACTIVE);
        verify(apiKeyRepo).save(record);
    }

    @Test
    void revokeAPIKey_returnsFalseWhenTenantDoesNotMatch() {
        UUID keyId = UUID.randomUUID();
        APIKey record = new APIKey();
        record.setId(keyId);
        record.setTenantId(UUID.randomUUID());
        when(apiKeyRepo.findById(keyId)).thenReturn(Optional.of(record));

        boolean result = apiKeyService.revokeAPIKey(keyId, UUID.randomUUID());

        assertThat(result).isFalse();
        verify(apiKeyRepo, never()).save(any());
    }

    @Test
    void getAPIKeys_usesFindByTenantIdWhenStatusIsNull() {
        UUID tenantId = UUID.randomUUID();
        APIKey record = new APIKey();
        when(apiKeyRepo.findByTenantId(tenantId)).thenReturn(List.of(record));

        List<APIKey> result = apiKeyService.getAPIKeys(tenantId, null);

        assertThat(result).containsExactly(record);
        verify(apiKeyRepo, never()).findByTenantIdAndStatus(any(), any());
    }

    @Test
    void getAPIKeys_usesFindByTenantIdAndStatusWhenStatusGiven() {
        UUID tenantId = UUID.randomUUID();
        APIKey record = new APIKey();
        when(apiKeyRepo.findByTenantIdAndStatus(tenantId, ApiKeyStatus.ACTIVE)).thenReturn(List.of(record));

        List<APIKey> result = apiKeyService.getAPIKeys(tenantId, ApiKeyStatus.ACTIVE);

        assertThat(result).containsExactly(record);
    }
}
