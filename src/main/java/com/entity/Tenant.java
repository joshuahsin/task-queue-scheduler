package com.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Email;

enum TenantStatus {
    ACTIVE,
    INACTIVE
}

@Entity
@Table(name = "tenants")
public class Tenant {
    private UUID id;
    private String name;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    private TenantStatus status;
    private int maxRetriesOverride;
    private Instant createdAt;
}
