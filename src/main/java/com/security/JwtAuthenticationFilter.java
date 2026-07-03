package com.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;

import com.exception.InvalidTokenException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            try {
                Claims claims = jwtUtil.parseClaims(token);
                String role = claims.get("role", String.class);
                // User tokens carry a role claim (ADMIN/USER) and a tenantId claim. API-key tokens
                // don't — they authenticate a tenant's backend service, not a specific human, so the
                // subject itself IS the tenantId, and they get a distinct SERVICE authority.
                String authority = role != null ? "ROLE_" + role : "ROLE_SERVICE";
                String tenantIdClaim = claims.get("tenantId", String.class);
                UUID tenantId = UUID.fromString(tenantIdClaim != null ? tenantIdClaim : claims.getSubject());

                AuthenticatedPrincipal principal = new AuthenticatedPrincipal(claims.getSubject(), tenantId);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority(authority)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenException | IllegalArgumentException e) {
                // Leave unauthenticated — the authorization rules decide whether that's acceptable.
            }
        }

        filterChain.doFilter(request, response);
    }
}
