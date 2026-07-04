package com.exception;

import java.util.UUID;

public class TenantMetricsNotFoundException extends RuntimeException {
    public TenantMetricsNotFoundException(UUID tenantId) {
        super("Metrics not found for tenant: " + tenantId);
    }
}
