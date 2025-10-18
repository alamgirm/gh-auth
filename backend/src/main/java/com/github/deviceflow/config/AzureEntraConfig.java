package com.github.deviceflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "azure.entra")
public class AzureEntraConfig {
    
    private String clientId;
    private String tenantId;
    private String authority;
    private String jwksUri;
    private String issuer;
    private String expectedAudience; // api://your-backend-client-id
}

