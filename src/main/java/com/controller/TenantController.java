package com.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.DTO.CreateTenantRequest;
import com.DTO.TenantMetricsResponse;
import com.DTO.TenantResponse;
import com.DTO.UpdateTenantRequest;
import com.service.TenantService;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<String> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        String tenantId = tenantService.createTenant(request.getName(), request.getPlanId());
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantId);
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deleteTenant(@PathVariable UUID tenantId) {
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(TenantResponse.from(tenantService.updateTenant(tenantId, request.getPlanId())));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(TenantResponse.from(tenantService.getTenant(tenantId)));
    }

    @GetMapping("/{tenantId}/metrics")
    public ResponseEntity<TenantMetricsResponse> getTenantMetrics(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(TenantMetricsResponse.from(tenantService.getTenantMetrics(tenantId)));
    }
}
