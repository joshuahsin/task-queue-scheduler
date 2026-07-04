package com.oci;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;

@Configuration
public class OciAuthConfig {

    @Bean
    public BasicAuthenticationDetailsProvider ociAuthenticationDetailsProvider(
            @Value("${oci.auth.method}") String authMethod) throws IOException {
        return switch (authMethod) {
            // For local dev, off the OCI VM — reads ~/.oci/config.
            case "config-file" -> new ConfigFileAuthenticationDetailsProvider("DEFAULT");
            // For the deployed compute instance — identity comes from the instance itself via an
            // IAM dynamic group policy, no credentials to manage.
            case "instance-principal" -> InstancePrincipalsAuthenticationDetailsProvider.builder().build();
            default -> throw new IllegalStateException("Unknown oci.auth.method: " + authMethod);
        };
    }
}
