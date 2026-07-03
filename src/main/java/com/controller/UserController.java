package com.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.DTO.APIKeyResponse;
import com.DTO.ApiKeyTokenRequest;
import com.DTO.AuthResponse;
import com.DTO.CreateAPIKeyRequest;
import com.DTO.CreateAPIKeyResponse;
import com.DTO.LoginRequest;
import com.DTO.RegisterRequest;
import com.enums.Enums.ApiKeyStatus;
import com.exception.APIKeyNotFoundException;
import com.exception.UserNotFoundException;
import com.security.AuthenticatedPrincipal;
import com.service.APIKeyService;
import com.service.UserService;

@RestController
@RequestMapping("/api/v1/auth")
public class UserController {
    private final UserService userService;
    private final APIKeyService apiKeyService;

    public UserController(UserService userService, APIKeyService apiKeyService) {
        this.userService = userService;
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request.getTenantId(), request.getEmail(), request.getPassword(), request.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new AuthResponse(token, "Bearer"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam UUID userId) {
        if (!userService.deleteAccount(userId, principal.tenantId())) {
            throw new UserNotFoundException(userId);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        String newToken = userService.refreshToken(refreshToken);
        return ResponseEntity.ok(new AuthResponse(newToken, "Bearer"));
    }

    @PostMapping("/api-keys")
    public ResponseEntity<CreateAPIKeyResponse> createAPIKey(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody CreateAPIKeyRequest request) {
        String key = apiKeyService.createAPIKey(principal.tenantId(), request.getName(), request.getExpiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateAPIKeyResponse(key));
    }

    @DeleteMapping("/api-keys/{apiKeyId}")
    public ResponseEntity<Void> revokeAPIKey(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID apiKeyId) {
        if (!apiKeyService.revokeAPIKey(apiKeyId, principal.tenantId())) {
            throw new APIKeyNotFoundException(apiKeyId);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api-keys")
    public ResponseEntity<List<APIKeyResponse>> getAPIKeys(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam(required = false) ApiKeyStatus status) {
        List<APIKeyResponse> keys = apiKeyService.getAPIKeys(principal.tenantId(), status).stream()
            .map(APIKeyResponse::from)
            .toList();
        return ResponseEntity.ok(keys);
    }

    @PostMapping("/api-keys/token")
    public ResponseEntity<AuthResponse> getApiKeyToken(@Valid @RequestBody ApiKeyTokenRequest request) {
        String token = apiKeyService.getToken(request.getApiKey());
        return ResponseEntity.ok(new AuthResponse(token, "Bearer"));
    }
}
