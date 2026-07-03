package com.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.entity.TenantMetrics;

public interface TenantMetricsRepo extends JpaRepository<TenantMetrics, UUID> {
}
