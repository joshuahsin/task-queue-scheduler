package com.taskqueue.task_queue_scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

// UserDetailsServiceAutoConfiguration excluded: auth is handled entirely by JwtAuthenticationFilter/SecurityConfig
// against our own JPA UserRepo, not Spring Security's UserDetailsService — leaving it in only generates an
// unused in-memory user + a misleading "generated security password" log line every startup.
//
// scanBasePackages="com" is required: every other package in this codebase (com.service, com.controller,
// com.repo, com.security, etc.) is a sibling of com.taskqueue.task_queue_scheduler, not a sub-package of it,
// so @SpringBootApplication's default component scan (main class's package + sub-packages only) would never
// discover any of them.
//
// @EnableJpaRepositories is required separately from scanBasePackages: Spring Data's repository-interface
// scanning is its own mechanism, not covered by the general component scan, so com.repo needs to be named
// explicitly here too or JpaRepository interfaces (TaskRepo, TenantRepo, etc.) never get proxied into beans.
//
// @EntityScan is a third, independent mechanism (same story): without it, com.entity classes are never
// registered as JPA-managed types, which is also why ddl-auto=validate silently "passed" in every earlier
// test — there was nothing registered to validate against the schema, not evidence it actually matched.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class, scanBasePackages = "com")
@EnableJpaRepositories(basePackages = "com.repo")
@EntityScan(basePackages = "com.entity")
@EnableScheduling
public class TaskQueueSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskQueueSchedulerApplication.class, args);
	}

}
