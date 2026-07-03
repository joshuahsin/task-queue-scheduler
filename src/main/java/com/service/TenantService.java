package com.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.DAO.TenantDAO;
import com.entity.Tenant;
import com.entity.TenantMetrics;
import com.enums.Enums.TenantStatus;
import com.repo.TenantMetricsRepo;
import com.repo.TenantRepo;

@Service
public class TenantService implements TenantDAO {
    private final TenantRepo tenantRepo;
    private final TenantMetricsRepo tenantMetricsRepo;

    public TenantService(TenantRepo tenantRepo, TenantMetricsRepo tenantMetricsRepo) {
        this.tenantRepo = tenantRepo;
        this.tenantMetricsRepo = tenantMetricsRepo;
    }

    @Override
    public Tenant createTenant(String name, UUID planId) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setPlanId(planId);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setCreatedAt(Instant.now());
        return tenantRepo.save(tenant);
    }

    @Override
    public boolean deleteTenant(UUID tenantId) {
        return tenantRepo.findById(tenantId)
            .map(tenant -> {
                tenantRepo.delete(tenant);
                return true;
            })
            .orElse(false);
    }

    @Override
    public Optional<Tenant> updateTenant(UUID tenantId, UUID planId) {
        return tenantRepo.findById(tenantId)
            .map(tenant -> {
                tenant.setPlanId(planId);
                return tenantRepo.save(tenant);
            });
    }

    @Override
    public Optional<Tenant> getTenant(UUID tenantId) {
        return tenantRepo.findById(tenantId);
    }

    @Override
    public Optional<TenantMetrics> getTenantMetrics(UUID tenantId) {
        return tenantMetricsRepo.findById(tenantId);
    }
}
