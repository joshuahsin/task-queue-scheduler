package com.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Bootstrap / credential-exchange endpoints — no principal exists yet
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                        "/api/v1/auth/refresh", "/api/v1/auth/api-keys/token").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/tenants").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/tenants/plans", "/api/v1/tenants/plans/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // No worker auth mechanism yet — left open intentionally
                .requestMatchers("/api/v1/workers/**").permitAll()

                // Admin-only management
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/api-keys").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/api-keys/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/account").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/tenants/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/tenants/**").hasRole("ADMIN")

                // Task mutations — admin users or API-key-authenticated services, not read-only USERs
                .requestMatchers(HttpMethod.POST, "/api/v1/tasks/**").hasAnyRole("ADMIN", "SERVICE")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/tasks/**").hasAnyRole("ADMIN", "SERVICE")

                // Everything else just needs a valid, authenticated caller of any type
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
