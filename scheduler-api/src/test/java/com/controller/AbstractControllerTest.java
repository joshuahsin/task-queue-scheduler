package com.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.security.JwtAuthenticationFilter;
import com.security.JwtUtil;
import com.security.SecurityConfig;
import com.service.APIKeyService;
import com.service.PlanService;
import com.service.TaskExecutionService;
import com.service.TaskService;
import com.service.TenantService;
import com.service.UserService;
import com.service.WorkerService;
import com.service.WorkerTaskService;

// Shared plumbing for @WebMvcTest-based controller tests. These tests are about controller/service
// wiring and HTTP status/response-shape behavior, not about SecurityConfig's routing rules — that's
// SecurityRoutingTest's job. The real security stack is still loaded (rather than disabled via
// addFilters=false) because @AuthenticationPrincipal needs a real Authentication populated by
// JwtAuthenticationFilter; subclasses just always authenticate as ADMIN, which passes every rule in
// SecurityConfig, so tests can focus purely on what happens once a request reaches the controller.
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
abstract class AbstractControllerTest {

    // Anchors @WebMvcTest's @SpringBootConfiguration auto-detection (see SecurityRoutingTest's
    // identical-in-spirit nested class for the full background on why this is needed at all).
    //
    // @WebMvcTest(controllers = X.class) only NARROWS an existing component scan down to X — it does
    // not independently import X regardless of scanning. So com.controller must be scanned here for
    // any controller to be found at all, and com.exception is scanned too for GlobalExceptionHandler
    // (a sibling package this anchor can't otherwise reach). The catch, confirmed empirically: once
    // com.controller is scanned this way (via a plain @ComponentScan on this nested class, not
    // @WebMvcTest's own internal scan), controllers= no longer narrows anything — every controller in
    // the package gets instantiated regardless of which one a given subclass names. That's exactly why
    // every service every controller depends on is mocked below, in the shared base, rather than only
    // the ones relevant to whichever controller a subclass is nominally testing.
    @SpringBootConfiguration
    @ComponentScan(basePackages = {"com.controller", "com.exception"})
    static class TestSpringBootConfig {
    }

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    protected TaskService taskService;
    @MockitoBean
    protected TaskExecutionService taskExecutionService;
    @MockitoBean
    protected WorkerService workerService;
    @MockitoBean
    protected WorkerTaskService workerTaskService;
    @MockitoBean
    protected TenantService tenantService;
    @MockitoBean
    protected PlanService planService;
    @MockitoBean
    protected UserService userService;
    @MockitoBean
    protected APIKeyService apiKeyService;

    protected String adminToken(UUID tenantId) {
        return userToken(UUID.randomUUID(), tenantId, "ADMIN");
    }

    // Mirrors UserService.generateUserToken's real shape: subject = userId, claims = tenantId + role.
    protected String userToken(UUID userId, UUID tenantId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId.toString());
        claims.put("role", role);
        return "Bearer " + jwtUtil.generateToken(userId.toString(), claims);
    }
}
