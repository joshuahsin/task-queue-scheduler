package com.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.entity.TenantMetrics;

public interface TenantMetricsRepo extends JpaRepository<TenantMetrics, UUID> {

    // Fire-and-forget: a brand-new tenant with no TenantMetrics row yet (before the first periodic
    // refresh) means this affects 0 rows, which is an acceptable no-op rather than something to
    // upsert around.
    @Modifying
    @Transactional
    @Query("UPDATE TenantMetrics m SET m.rateLimitHits = m.rateLimitHits + 1 WHERE m.tenantId = :tenantId")
    int incrementRateLimitHits(@Param("tenantId") UUID tenantId);
}
