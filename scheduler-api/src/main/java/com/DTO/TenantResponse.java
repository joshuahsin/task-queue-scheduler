package com.DTO;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.entity.Tenant;
import com.enums.Enums.TenantStatus;

@Getter
@AllArgsConstructor
public class TenantResponse {
    private UUID id;
    private String name;
    private UUID planId;
    private TenantStatus status;
    private Instant createdAt;

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
            tenant.getId(), tenant.getName(), tenant.getPlanId(),
            tenant.getStatus(), tenant.getCreatedAt()
        );
    }
}
