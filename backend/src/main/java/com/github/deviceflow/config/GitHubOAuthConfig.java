package com.github.deviceflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "github.oauth")
public class GitHubOAuthConfig {
    
    private String clientId;
    private String clientSecret;
    private String deviceCodeUrl;
    private String tokenUrl;
    private String userApiUrl;
}

