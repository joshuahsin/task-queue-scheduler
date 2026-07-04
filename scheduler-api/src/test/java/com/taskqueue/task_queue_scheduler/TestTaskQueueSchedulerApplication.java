package com.taskqueue.task_queue_scheduler;

import org.springframework.boot.SpringApplication;

public class TestTaskQueueSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.from(TaskQueueSchedulerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
