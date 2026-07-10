package com.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;

import com.entity.Plan;
import com.entity.Tenant;
import com.entity.TenantMetrics;
import com.enums.Enums.PlanTier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantController.class)
class TenantControllerTest extends AbstractControllerTest {

    @Test
    void getPlans_returns200() throws Exception {
        Plan plan = new Plan();
        plan.setTier(PlanTier.FREE);
        when(planService.getPlans()).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/v1/tenants/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tier").value("FREE"));
    }

    @Test
    void getPlanByTier_returns200WhenFound() throws Exception {
        Plan plan = new Plan();
        plan.setTier(PlanTier.PREMIUM);
        when(planService.getPlanByTier(PlanTier.PREMIUM)).thenReturn(Optional.of(plan));

        mockMvc.perform(get("/api/v1/tenants/plans/PREMIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("PREMIUM"));
    }

    @Test
    void getPlanByTier_returns404WhenNotFound() throws Exception {
        when(planService.getPlanByTier(PlanTier.FREE)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tenants/plans/FREE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTenant_returns201() throws Exception {
        UUID planId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("acme");
        when(tenantService.createTenant("acme", planId)).thenReturn(tenant);

        mockMvc.perform(post("/api/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"acme\",\"planId\":\"" + planId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("acme"));
    }

    @Test
    void createTenant_returns400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"planId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTenant_returns204WhenOwnTenantAndFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.deleteTenant(tenantId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/tenants/" + tenantId).header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTenant_returns404WhenServiceReportsNotFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.deleteTenant(tenantId)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/tenants/" + tenantId).header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTenant_returns404WhenCallerBelongsToDifferentTenant() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID callerTenantId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/tenants/" + tenantId).header("Authorization", adminToken(callerTenantId)))
                .andExpect(status().isNotFound());

        // The ownership check happens before the service is ever consulted — a caller can't tell
        // "not my tenant" apart from "doesn't exist" even by side-channel (e.g. was deleteTenant
        // actually invoked).
        verify(tenantService, never()).deleteTenant(any());
    }

    @Test
    void updateTenant_returns200WhenOwnTenantAndFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID newPlanId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        when(tenantService.updateTenant(tenantId, newPlanId)).thenReturn(Optional.of(tenant));

        mockMvc.perform(put("/api/v1/tenants/" + tenantId)
                        .header("Authorization", adminToken(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + newPlanId + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateTenant_returns404WhenServiceReportsNotFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.updateTenant(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/tenants/" + tenantId)
                        .header("Authorization", adminToken(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTenant_returns404WhenCallerBelongsToDifferentTenant() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/tenants/" + tenantId)
                        .header("Authorization", adminToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());

        verify(tenantService, never()).updateTenant(any(), any());
    }

    @Test
    void getTenant_returns200WhenOwnTenantAndFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("acme");
        when(tenantService.getTenant(tenantId)).thenReturn(Optional.of(tenant));

        mockMvc.perform(get("/api/v1/tenants/" + tenantId).header("Authorization", adminToken(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("acme"));
    }

    @Test
    void getTenant_returns404WhenCallerBelongsToDifferentTenant() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/tenants/" + tenantId).header("Authorization", adminToken(UUID.randomUUID())))
                .andExpect(status().isNotFound());

        verify(tenantService, never()).getTenant(any());
    }

    @Test
    void getTenantMetrics_returns200WhenOwnTenantAndFound() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantMetrics metrics = new TenantMetrics();
        metrics.setTenantId(tenantId);
        metrics.setTotalTasks(5);
        when(tenantService.getTenantMetrics(tenantId)).thenReturn(Optional.of(metrics));

        mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/metrics").header("Authorization", adminToken(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(5));
    }

    @Test
    void getTenantMetrics_returns404WhenMetricsNotComputedYet() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.getTenantMetrics(tenantId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/metrics").header("Authorization", adminToken(tenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTenantMetrics_returns404WhenCallerBelongsToDifferentTenant() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/metrics").header("Authorization", adminToken(UUID.randomUUID())))
                .andExpect(status().isNotFound());

        verify(tenantService, never()).getTenantMetrics(any());
    }
}
