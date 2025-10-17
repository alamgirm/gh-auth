package com.github.deviceflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "github.app")
public class GitHubOAuthConfig {
    
    private String clientId;
    private String clientSecret;
    private String authorizeUrl;
    private String tokenUrl;
    private String userApiUrl;
    private String redirectUri;
}

