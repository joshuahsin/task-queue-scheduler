package com.DAO;

import java.util.UUID;

import com.entity.Tenant;
import com.entity.TenantMetrics;

public interface TenantDAO {
    public String createTenant(String name, UUID planId);
    public boolean deleteTenant(UUID tenantId);
    public Tenant updateTenant(UUID tenantId, UUID planId);
    public Tenant getTenant(UUID tenantId);
    public TenantMetrics getTenantMetrics(UUID tenantId);
}
