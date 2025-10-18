package com.github.deviceflow.service;

import com.github.deviceflow.config.GhecOAuthConfig;
import com.github.deviceflow.config.GhesOAuthConfig;
import com.github.deviceflow.entity.UserToken;
import com.github.deviceflow.model.AccessTokenResponse;
import com.github.deviceflow.model.GitHubUser;
import com.github.deviceflow.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubAuthService {
    
    private final GhecOAuthConfig ghecConfig;
    private final GhesOAuthConfig ghesConfig;
    private final WebClient.Builder webClientBuilder;
    private final UserTokenRepository userTokenRepository;
    
    /**
     * Get config values for a provider
     */
    private Map<String, String> getProviderConfig(String provider) {
        Map<String, String> config = new HashMap<>();
        
        if ("ghes".equals(provider)) {
            config.put("clientId", ghesConfig.getClientId());
            config.put("clientSecret", ghesConfig.getClientSecret());
            config.put("authorizeUrl", ghesConfig.getAuthorizeUrl());
            config.put("tokenUrl", ghesConfig.getTokenUrl());
            config.put("userApiUrl", ghesConfig.getUserApiUrl());
            config.put("redirectUri", ghesConfig.getRedirectUri());
            config.put("provider", "ghes");
        } else {
            // Default to ghec
            config.put("clientId", ghecConfig.getClientId());
            config.put("clientSecret", ghecConfig.getClientSecret());
            config.put("authorizeUrl", ghecConfig.getAuthorizeUrl());
            config.put("tokenUrl", ghecConfig.getTokenUrl());
            config.put("userApiUrl", ghecConfig.getUserApiUrl());
            config.put("redirectUri", ghecConfig.getRedirectUri());
            config.put("provider", "ghec");
        }
        
        return config;
    }
    
    /**
     * Generate the GitHub authorization URL for popup-based login
     */
    public String getAuthorizationUrl(String provider, String state) {
        log.info("Generating authorization URL for provider: {}", provider);
        
        Map<String, String> config = getProviderConfig(provider);
        String scope = URLEncoder.encode("user:email read:user", StandardCharsets.UTF_8);
        
        return String.format("%s?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                config.get("authorizeUrl"),
                config.get("clientId"),
                URLEncoder.encode(config.get("redirectUri"), StandardCharsets.UTF_8),
                scope,
                state);
    }
    
    /**
     * Exchange code for token WITHOUT storing (for account linking)
     * Returns both token and user info
     */
    public Map<String, Object> exchangeCodeWithoutStoring(String provider, String code) {
        log.info("Exchanging {} code for token (no storage)", provider);
        
        Map<String, String> config = getProviderConfig(provider);
        WebClient webClient = webClientBuilder.build();
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", config.get("clientId"));
        formData.add("client_secret", config.get("clientSecret"));
        formData.add("code", code);
        formData.add("redirect_uri", config.get("redirectUri"));
        
        try {
            AccessTokenResponse response = webClient.post()
                    .uri(config.get("tokenUrl"))
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(AccessTokenResponse.class)
                    .block();
            
            if (response != null && response.getAccessToken() != null) {
                log.info("Successfully exchanged code for token");
                
                // Fetch user info
                GitHubUser user = fetchUserInfoWithToken(config.get("userApiUrl"), response.getAccessToken());
                
                if (user == null) {
                    throw new RuntimeException("Failed to fetch user info");
                }
                
                log.info("{} user: {} ({})", provider.toUpperCase(), user.getLogin(), user.getId());
                
                // Return both without storing
                Map<String, Object> result = new HashMap<>();
                result.put("token", response.getAccessToken());
                result.put("user", user);
                result.put("userId", String.valueOf(user.getId()));
                result.put("provider", provider);
                
                return result;
                
            } else if (response != null && response.getError() != null) {
                log.error("Error from {}: {} - {}", provider, response.getError(), response.getErrorDescription());
                throw new RuntimeException(provider.toUpperCase() + " OAuth error: " + response.getError());
            }
            
        } catch (WebClientResponseException e) {
            log.error("Error exchanging code for token: {} - {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to exchange code for token: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during token exchange", e);
            throw new RuntimeException("Failed to exchange code for token: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to exchange code for token");
    }
    
    /**
     * Fetch user information from GitHub API using access token
     */
    private GitHubUser fetchUserInfoWithToken(String userApiUrl, String accessToken) {
        log.debug("Fetching user info from: {}", userApiUrl);
        
        WebClient webClient = webClientBuilder.build();
        
        try {
            GitHubUser user = webClient.get()
                    .uri(userApiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(GitHubUser.class)
                    .block();
            
            log.info("Successfully fetched user: {}", user != null ? user.getLogin() : "unknown");
            return user;
            
        } catch (Exception e) {
            log.error("Error fetching user info", e);
            throw new RuntimeException("Failed to fetch user info: " + e.getMessage());
        }
    }
    
    /**
     * Fetch user info using a stored token
     */
    public GitHubUser fetchUserInfoWithStoredToken(String provider, String token) {
        log.debug("Fetching {} user info with provided token", provider);
        
        Map<String, String> config = getProviderConfig(provider);
        WebClient webClient = webClientBuilder.build();
        
        try {
            GitHubUser user = webClient.get()
                    .uri(config.get("userApiUrl"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(GitHubUser.class)
                    .block();
            
            log.info("Successfully fetched {} user: {}", provider, user != null ? user.getLogin() : "unknown");
            return user;
            
        } catch (Exception e) {
            log.error("Error fetching {} user info", provider, e);
            throw new RuntimeException("Failed to fetch " + provider + " user info: " + e.getMessage());
        }
    }
    
    /**
     * Check if Ghes is configured
     */
    public boolean isGhesEnabled() {
        return ghesConfig.getClientId() != null && 
               !ghesConfig.getClientId().isEmpty() &&
               ghesConfig.getBaseUrl() != null &&
               !ghesConfig.getBaseUrl().isEmpty();
    }
}
