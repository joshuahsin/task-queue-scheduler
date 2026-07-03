package com.taskqueue.task_queue_scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TaskQueueSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskQueueSchedulerApplication.class, args);
	}

}
