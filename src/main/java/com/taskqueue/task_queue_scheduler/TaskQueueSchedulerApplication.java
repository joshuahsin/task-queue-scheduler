package com.taskqueue.task_queue_scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// UserDetailsServiceAutoConfiguration excluded: auth is handled entirely by JwtAuthenticationFilter/SecurityConfig
// against our own JPA UserRepo, not Spring Security's UserDetailsService — leaving it in only generates an
// unused in-memory user + a misleading "generated security password" log line every startup.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class TaskQueueSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskQueueSchedulerApplication.class, args);
	}

}
