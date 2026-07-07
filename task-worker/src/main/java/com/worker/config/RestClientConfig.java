package com.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient schedulerApiRestClient(WorkerProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getSchedulerApiBaseUrl())
                .build();
    }
}
