package com.DAO;

import java.util.Optional;
import java.util.UUID;

import com.entity.Tenant;
import com.entity.TenantMetrics;

public interface TenantDAO {
    public Tenant createTenant(String name, UUID planId);
    public boolean deleteTenant(UUID tenantId);
    public Optional<Tenant> updateTenant(UUID tenantId, UUID planId);
    public Optional<Tenant> getTenant(UUID tenantId);
    public Optional<TenantMetrics> getTenantMetrics(UUID tenantId);
}
