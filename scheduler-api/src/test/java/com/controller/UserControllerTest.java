package com.controller;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;

import com.entity.APIKey;
import com.enums.Enums.ApiKeyStatus;
import com.exception.InvalidApiKeyException;
import com.exception.InvalidCredentialsException;
import com.exception.InvalidTokenException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest extends AbstractControllerTest {

    @Test
    void register_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","email":"a@b.com","password":"hunter2","role":"USER"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());
    }

    @Test
    void register_returns400WhenEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","email":"not-an-email","password":"hunter2","role":"USER"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns200WithToken() throws Exception {
        when(userService.login("a@b.com", "hunter2")).thenReturn("jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"hunter2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    // Distinct from SecurityConfig's routing-level 401/403 (covered in SecurityRoutingTest) — this
    // 401 comes from GlobalExceptionHandler mapping a business exception (bad credentials), not from
    // Spring Security's filter chain rejecting an unauthenticated/under-authorized request.
    @Test
    void login_returns401WhenCredentialsInvalid() throws Exception {
        when(userService.login(any(), any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAccount_returns204() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userService.deleteAccount(userId, tenantId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/auth/account?userId=" + userId).header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAccount_returns404WhenNotFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userService.deleteAccount(userId, tenantId)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/auth/account?userId=" + userId).header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void refreshToken_returns200() throws Exception {
        when(userService.refreshToken("old-token")).thenReturn("new-token");

        mockMvc.perform(post("/api/v1/auth/refresh").param("refreshToken", "old-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-token"));
    }

    @Test
    void refreshToken_returns401WhenTokenInvalid() throws Exception {
        when(userService.refreshToken("bad-token")).thenThrow(new InvalidTokenException("Invalid or expired token"));

        mockMvc.perform(post("/api/v1/auth/refresh").param("refreshToken", "bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAPIKey_returns201() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(apiKeyService.createAPIKey(any(), any(), any())).thenReturn("sk_rawkey");

        mockMvc.perform(post("/api/v1/auth/api-keys")
                        .header("Authorization", adminToken(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ci-key\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("sk_rawkey"));
    }

    @Test
    void createAPIKey_returns400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/api-keys")
                        .header("Authorization", adminToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void revokeAPIKey_returns204() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID apiKeyId = UUID.randomUUID();
        when(apiKeyService.revokeAPIKey(apiKeyId, tenantId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/auth/api-keys/" + apiKeyId).header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void revokeAPIKey_returns404WhenNotFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID apiKeyId = UUID.randomUUID();
        when(apiKeyService.revokeAPIKey(apiKeyId, tenantId)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/auth/api-keys/" + apiKeyId).header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAPIKeys_withoutStatusParam_passesNullStatus() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(apiKeyService.getAPIKeys(tenantId, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/auth/api-keys").header("Authorization", adminToken(tenantId)))
                .andExpect(status().isOk());

        verify(apiKeyService).getAPIKeys(tenantId, null);
    }

    @Test
    void getAPIKeys_withStatusParam_passesItThrough() throws Exception {
        UUID tenantId = UUID.randomUUID();
        APIKey key = new APIKey();
        key.setStatus(ApiKeyStatus.ACTIVE);
        when(apiKeyService.getAPIKeys(tenantId, ApiKeyStatus.ACTIVE)).thenReturn(List.of(key));

        mockMvc.perform(get("/api/v1/auth/api-keys")
                        .header("Authorization", adminToken(tenantId))
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getApiKeyToken_returns200() throws Exception {
        when(apiKeyService.getToken("sk_valid")).thenReturn("jwt-token");

        mockMvc.perform(post("/api/v1/auth/api-keys/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"sk_valid\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void getApiKeyToken_returns401WhenKeyInvalid() throws Exception {
        when(apiKeyService.getToken("sk_bogus")).thenThrow(new InvalidApiKeyException("Invalid API key"));

        mockMvc.perform(post("/api/v1/auth/api-keys/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"sk_bogus\"}"))
                .andExpect(status().isUnauthorized());
    }
}
