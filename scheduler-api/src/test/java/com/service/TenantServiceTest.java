package com.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.entity.Tenant;
import com.entity.TenantMetrics;
import com.enums.Enums.TenantStatus;
import com.repo.TenantMetricsRepo;
import com.repo.TenantRepo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepo tenantRepo;
    @Mock
    private TenantMetricsRepo tenantMetricsRepo;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantRepo, tenantMetricsRepo);
    }

    @Test
    void createTenant_savesActiveTenantWithGivenNameAndPlan() {
        UUID planId = UUID.randomUUID();
        when(tenantRepo.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant tenant = tenantService.createTenant("acme", planId);

        assertThat(tenant.getName()).isEqualTo("acme");
        assertThat(tenant.getPlanId()).isEqualTo(planId);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.getCreatedAt()).isNotNull();
    }

    @Test
    void deleteTenant_deletesAndReturnsTrueWhenFound() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));

        boolean result = tenantService.deleteTenant(tenantId);

        assertThat(result).isTrue();
        verify(tenantRepo).delete(tenant);
    }

    @Test
    void deleteTenant_returnsFalseWhenNotFound() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.empty());

        assertThat(tenantService.deleteTenant(tenantId)).isFalse();
        verify(tenantRepo, never()).delete(any());
    }

    @Test
    void updateTenant_updatesPlanIdWhenFound() {
        UUID tenantId = UUID.randomUUID();
        UUID newPlanId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setPlanId(UUID.randomUUID());
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepo.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Tenant> result = tenantService.updateTenant(tenantId, newPlanId);

        assertThat(result).isPresent();
        assertThat(result.get().getPlanId()).isEqualTo(newPlanId);
    }

    @Test
    void updateTenant_returnsEmptyWhenNotFound() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.empty());

        assertThat(tenantService.updateTenant(tenantId, UUID.randomUUID())).isEmpty();
    }

    @Test
    void getTenant_delegatesToRepo() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThat(tenantService.getTenant(tenantId)).contains(tenant);
    }

    @Test
    void getTenantMetrics_delegatesToMetricsRepo() {
        UUID tenantId = UUID.randomUUID();
        TenantMetrics metrics = new TenantMetrics();
        when(tenantMetricsRepo.findById(tenantId)).thenReturn(Optional.of(metrics));

        assertThat(tenantService.getTenantMetrics(tenantId)).contains(metrics);
    }
}
