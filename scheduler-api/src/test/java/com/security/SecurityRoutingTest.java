package com.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.controller.TaskController;
import com.controller.TenantController;
import com.controller.UserController;
import com.controller.WorkerController;
import com.entity.Task;
import com.entity.Tenant;
import com.service.APIKeyService;
import com.service.PlanService;
import com.service.TaskExecutionService;
import com.service.TaskService;
import com.service.TenantService;
import com.service.UserService;
import com.service.WorkerService;
import com.service.WorkerTaskService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Verifies SecurityConfig's route-authorization rules and JwtAuthenticationFilter's token
// handling actually take effect end-to-end — something no plain unit test can exercise, since it
// depends on a real request being dispatched through the real filter chain. The real SecurityConfig,
// JwtAuthenticationFilter and JwtUtil are loaded (@Import); every controller dependency is mocked,
// since only the security *routing* behavior is in scope here, not controller/service business logic
// (that's what the service-layer unit tests and any future controller-behavior tests cover).
@WebMvcTest(controllers = {TaskController.class, WorkerController.class, TenantController.class, UserController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
class SecurityRoutingTest {

    // Anchors @WebMvcTest's @SpringBootConfiguration auto-detection. The real main class,
    // TaskQueueSchedulerApplication, lives in a sibling package (com.taskqueue.task_queue_scheduler,
    // not a parent of com.security) so upward-package search can't find it — and even pointing
    // @ContextConfiguration straight at it would drag in its class-level @EnableJpaRepositories/
    // @EntityScan, which this JPA-less web slice has no entityManagerFactory bean to satisfy.
    //
    // @ComponentScan is required for the same reason the real application class needs
    // scanBasePackages="com": com.controller is a sibling of this nested class's own package
    // (com.security), not a sub-package of it, so @WebMvcTest's default component-scan-based
    // controller discovery would otherwise never find the controllers even though they're named
    // explicitly in @WebMvcTest(controllers = ...). Scoped to just com.controller/com.exception
    // (not the whole "com" tree) to avoid sweeping in com.oci's real-OCI-SDK-backed beans, and to
    // avoid double-registering JwtAuthenticationFilter (already @Import-ed above, and it would also
    // separately match @WebMvcTest's own Filter-type allow-list if com.security were scanned too).
    @SpringBootConfiguration
    @ComponentScan(basePackages = {"com.controller", "com.exception"})
    static class TestSpringBootConfig {
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TaskService taskService;
    @MockitoBean
    private TaskExecutionService taskExecutionService;
    @MockitoBean
    private WorkerService workerService;
    @MockitoBean
    private WorkerTaskService workerTaskService;
    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private PlanService planService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private APIKeyService apiKeyService;

    // Mirrors UserService.generateUserToken's real shape: subject = userId, claims = tenantId + role.
    private String userToken(UUID userId, UUID tenantId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId.toString());
        claims.put("role", role);
        return "Bearer " + jwtUtil.generateToken(userId.toString(), claims);
    }

    // Mirrors APIKeyService.getToken's real shape: subject IS the tenantId, no role/tenantId claims
    // at all — JwtAuthenticationFilter treats that absence of a role claim as the SERVICE authority.
    private String serviceToken(UUID tenantId) {
        return "Bearer " + jwtUtil.generateToken(tenantId);
    }

    // ---- permitAll routes ----

    @Test
    void login_isReachableWithoutAnyToken() throws Exception {
        when(userService.login(any(), any())).thenReturn("jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"hunter2\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void register_isReachableWithoutAnyToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(notBlockedBySecurity());
    }

    @Test
    void refresh_isReachableWithoutAnyToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").param("refreshToken", "whatever"))
                .andExpect(notBlockedBySecurity());
    }

    @Test
    void apiKeyToken_isReachableWithoutAnyToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/api-keys/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"sk_whatever\"}"))
                .andExpect(notBlockedBySecurity());
    }

    @Test
    void createTenant_isReachableWithoutAnyToken() throws Exception {
        mockMvc.perform(post("/api/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(notBlockedBySecurity());
    }

    @Test
    void tenantPlans_isReachableWithoutAnyToken() throws Exception {
        when(planService.getPlans()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tenants/plans"))
                .andExpect(status().isOk());
    }

    @Test
    void workers_isReachableWithoutAnyToken() throws Exception {
        when(workerService.getWorkers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/workers"))
                .andExpect(status().isOk());
    }

    // ---- ADMIN-only routes ----
    //
    // Note: a request with no token at all also comes back 403, not 401, in every test below.
    // SecurityConfig disables both httpBasic and formLogin, so no AuthenticationEntryPoint is ever
    // registered to issue a 401 challenge; Spring Security's AnonymousAuthenticationFilter fills in
    // an anonymous principal for the missing-token case, and hasRole(...)/authenticated() checks
    // against that anonymous principal fail as AccessDeniedException (403), not AuthenticationException
    // (401) — so "no token" and "wrong role" are currently indistinguishable by status code alone.

    @Test
    void createApiKey_requiresAdminRole() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String body = "{\"name\":\"ci-key\"}";

        mockMvc.perform(post("/api/v1/auth/api-keys").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/api-keys").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "USER")))
                .andExpect(status().isForbidden());

        when(apiKeyService.createAPIKey(any(), any(), any())).thenReturn("sk_rawkey");
        mockMvc.perform(post("/api/v1/auth/api-keys").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "ADMIN")))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteAccount_requiresAdminRole() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/auth/account?userId=" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/auth/account?userId=" + UUID.randomUUID())
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "USER")))
                .andExpect(status().isForbidden());

        when(userService.deleteAccount(any(), any())).thenReturn(true);
        mockMvc.perform(delete("/api/v1/auth/account?userId=" + UUID.randomUUID())
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateTenant_requiresAdminRole() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String body = "{\"planId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(put("/api/v1/tenants/" + tenantId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/tenants/" + tenantId).contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "USER")))
                .andExpect(status().isForbidden());

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        when(tenantService.updateTenant(any(), any())).thenReturn(Optional.of(tenant));
        mockMvc.perform(put("/api/v1/tenants/" + tenantId).contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTenant_requiresAdminRole() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/tenants/" + tenantId))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/tenants/" + tenantId)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "USER")))
                .andExpect(status().isForbidden());

        when(tenantService.deleteTenant(any())).thenReturn(true);
        mockMvc.perform(delete("/api/v1/tenants/" + tenantId)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "ADMIN")))
                .andExpect(status().isNoContent());
    }

    // ---- ADMIN-or-SERVICE routes ----

    @Test
    void queueTask_allowsAdminAndServiceButNotPlainUser() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String body = "{\"type\":\"SIMULATED_WORK\",\"priority\":\"HIGH_PRIORITY\"}";

        mockMvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "USER")))
                .andExpect(status().isForbidden());

        when(taskService.queueTask(any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        mockMvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "ADMIN")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", serviceToken(tenantId)))
                .andExpect(status().isCreated());
    }

    @Test
    void cancelTask_allowsAdminAndServiceButNotPlainUser() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/tasks/" + taskId))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/tasks/" + taskId)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "USER")))
                .andExpect(status().isForbidden());

        when(taskService.cancelTask(any(), any())).thenReturn(true);
        mockMvc.perform(delete("/api/v1/tasks/" + taskId)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "ADMIN")))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/tasks/" + taskId)
                        .header("Authorization", serviceToken(tenantId)))
                .andExpect(status().isNoContent());
    }

    // ---- anyRequest().authenticated() fallback ----

    @Test
    void getTask_requiresAnyAuthenticatedCallerRegardlessOfRole() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTenantId(tenantId);

        mockMvc.perform(get("/api/v1/tasks/" + taskId))
                .andExpect(status().isForbidden());

        when(taskService.getTask(taskId, tenantId)).thenReturn(Optional.of(task));
        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "USER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", userToken(UUID.randomUUID(), tenantId, "ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", serviceToken(tenantId)))
                .andExpect(status().isOk());
    }

    // ---- JwtAuthenticationFilter's own token handling ----

    @Test
    void malformedBearerToken_isTreatedTheSameAsNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/" + UUID.randomUUID())
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isForbidden());
    }

    // Any status other than 401/403 proves the request cleared the security layer — Spring
    // Security's own filters short-circuit to those two codes before the request ever reaches a
    // controller, so whatever happens next (2xx, 400 from validation, 404, ...) is decisive proof
    // the security rule for this route is permitAll.
    private static org.springframework.test.web.servlet.ResultMatcher notBlockedBySecurity() {
        return result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }
}
