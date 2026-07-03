package com.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.DTO.CreateTenantRequest;
import com.DTO.PlanResponse;
import com.DTO.TenantMetricsResponse;
import com.DTO.TenantResponse;
import com.DTO.UpdateTenantRequest;
import com.entity.Tenant;
import com.enums.Enums.PlanTier;
import com.exception.PlanNotFoundException;
import com.exception.TenantMetricsNotFoundException;
import com.exception.TenantNotFoundException;
import com.security.AuthenticatedPrincipal;
import com.service.PlanService;
import com.service.TenantService;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    private final TenantService tenantService;
    private final PlanService planService;

    public TenantController(TenantService tenantService, PlanService planService) {
        this.tenantService = tenantService;
        this.planService = planService;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponse>> getPlans() {
        List<PlanResponse> plans = planService.getPlans().stream()
            .map(PlanResponse::from)
            .toList();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/plans/{tier}")
    public ResponseEntity<PlanResponse> getPlanByTier(@PathVariable PlanTier tier) {
        PlanResponse response = planService.getPlanByTier(tier)
            .map(PlanResponse::from)
            .orElseThrow(() -> new PlanNotFoundException(tier));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenant = tenantService.createTenant(request.getName(), request.getPlanId());
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(tenant));
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deleteTenant(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID tenantId) {
        requireOwnTenant(principal, tenantId);
        if (!tenantService.deleteTenant(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> updateTenant(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantRequest request) {
        requireOwnTenant(principal, tenantId);
        TenantResponse response = tenantService.updateTenant(tenantId, request.getPlanId())
            .map(TenantResponse::from)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenant(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID tenantId) {
        requireOwnTenant(principal, tenantId);
        TenantResponse response = tenantService.getTenant(tenantId)
            .map(TenantResponse::from)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tenantId}/metrics")
    public ResponseEntity<TenantMetricsResponse> getTenantMetrics(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID tenantId) {
        requireOwnTenant(principal, tenantId);
        TenantMetricsResponse response = tenantService.getTenantMetrics(tenantId)
            .map(TenantMetricsResponse::from)
            .orElseThrow(() -> new TenantMetricsNotFoundException(tenantId));
        return ResponseEntity.ok(response);
    }

    // A mismatch is treated identically to "doesn't exist" — a caller shouldn't be able to
    // learn that a tenant they don't belong to exists just by probing IDs.
    private void requireOwnTenant(AuthenticatedPrincipal principal, UUID tenantId) {
        if (!principal.tenantId().equals(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
    }
}
