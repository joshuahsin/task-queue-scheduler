package com.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.worker.config.WorkerProperties;

@SpringBootApplication
@EnableConfigurationProperties(WorkerProperties.class)
@EnableScheduling
public class TaskWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskWorkerApplication.class, args);
    }
}
