package com.github.deviceflow.service;

import com.github.deviceflow.config.GitHubOAuthConfig;
import com.github.deviceflow.model.AccessTokenResponse;
import com.github.deviceflow.model.GitHubUser;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubAuthService {
    
    private final GitHubOAuthConfig config;
    private final WebClient.Builder webClientBuilder;
    
    /**
     * Generate the GitHub authorization URL for popup-based login
     */
    public String getAuthorizationUrl(String state) {
        log.info("Generating authorization URL with state: {}", state);
        
        String scope = URLEncoder.encode("user:email read:user", StandardCharsets.UTF_8);
        
        return String.format("%s?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                config.getAuthorizeUrl(),
                config.getClientId(),
                URLEncoder.encode(config.getRedirectUri(), StandardCharsets.UTF_8),
                scope,
                state);
    }
    
    /**
     * Exchange authorization code for access token
     */
    public AccessTokenResponse exchangeCodeForToken(String code) {
        log.info("Exchanging authorization code for access token");
        
        WebClient webClient = webClientBuilder.build();
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", config.getClientId());
        formData.add("client_secret", config.getClientSecret());
        formData.add("code", code);
        formData.add("redirect_uri", config.getRedirectUri());
        
        try {
            AccessTokenResponse response = webClient.post()
                    .uri(config.getTokenUrl())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(AccessTokenResponse.class)
                    .block();
            
            if (response != null && response.getAccessToken() != null) {
                log.info("Successfully exchanged code for access token");
                return response;
            } else if (response != null && response.getError() != null) {
                log.error("Error from GitHub: {} - {}", response.getError(), response.getErrorDescription());
                throw new RuntimeException("GitHub OAuth error: " + response.getError());
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
     * Fetch user information from GitHub API
     */
    public GitHubUser fetchUserInfo(String accessToken) {
        log.debug("Fetching user info with access token");
        
        WebClient webClient = webClientBuilder.build();
        
        try {
            GitHubUser user = webClient.get()
                    .uri(config.getUserApiUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(GitHubUser.class)
                    .block();
            
            log.info("Successfully fetched user info for: {}", user != null ? user.getLogin() : "unknown");
            return user;
            
        } catch (Exception e) {
            log.error("Error fetching user info", e);
            throw new RuntimeException("Failed to fetch user info: " + e.getMessage());
        }
    }
    
    /**
     * Verify an access token and get user info
     */
    public GitHubUser verifyToken(String accessToken) {
        return fetchUserInfo(accessToken);
    }
}

