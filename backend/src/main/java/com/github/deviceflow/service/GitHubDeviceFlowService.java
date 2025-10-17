package com.github.deviceflow.service;

import com.github.deviceflow.config.GitHubOAuthConfig;
import com.github.deviceflow.model.AccessTokenResponse;
import com.github.deviceflow.model.DeviceCodeResponse;
import com.github.deviceflow.model.GitHubUser;
import com.github.deviceflow.model.PollStatusResponse;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubDeviceFlowService {
    
    private final GitHubOAuthConfig config;
    private final WebClient.Builder webClientBuilder;
    
    // In-memory storage for device codes (in production, use Redis or similar)
    private final Map<String, DeviceCodeResponse> deviceCodeStore = new ConcurrentHashMap<>();
    
    /**
     * Step 1: Request device and user codes from GitHub
     */
    public DeviceCodeResponse initiateDeviceFlow() {
        log.info("Initiating GitHub device flow");
        
        WebClient webClient = webClientBuilder.build();
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", config.getClientId());
        formData.add("scope", "user:email read:user");
        
        try {
            DeviceCodeResponse response = webClient.post()
                    .uri(config.getDeviceCodeUrl())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(DeviceCodeResponse.class)
                    .block();
            
            if (response != null) {
                // Store the device code for later polling
                deviceCodeStore.put(response.getDeviceCode(), response);
                log.info("Device flow initiated. User code: {}", response.getUserCode());
                return response;
            }
            
        } catch (WebClientResponseException e) {
            log.error("Error initiating device flow: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to initiate device flow: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during device flow initiation", e);
            throw new RuntimeException("Failed to initiate device flow: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to initiate device flow");
    }
    
    /**
     * Step 2: Poll GitHub to check if user has authorized the device
     */
    public PollStatusResponse pollForAuthorization(String deviceCode) {
        log.debug("Polling for authorization with device code: {}", deviceCode);
        
        DeviceCodeResponse deviceCodeData = deviceCodeStore.get(deviceCode);
        if (deviceCodeData == null) {
            return PollStatusResponse.builder()
                    .status("error")
                    .message("Invalid device code")
                    .build();
        }
        
        WebClient webClient = webClientBuilder.build();
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", config.getClientId());
        formData.add("device_code", deviceCode);
        formData.add("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
        
        try {
            AccessTokenResponse tokenResponse = webClient.post()
                    .uri(config.getTokenUrl())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(AccessTokenResponse.class)
                    .block();
            
            if (tokenResponse != null) {
                if (tokenResponse.getAccessToken() != null) {
                    // Authorization successful!
                    log.info("Authorization successful for device code: {}", deviceCode);
                    
                    // Fetch user info
                    GitHubUser user = fetchUserInfo(tokenResponse.getAccessToken());
                    
                    // Clean up the device code
                    deviceCodeStore.remove(deviceCode);
                    
                    return PollStatusResponse.builder()
                            .status("authorized")
                            .accessToken(tokenResponse.getAccessToken())
                            .user(user)
                            .message("Authorization successful")
                            .build();
                    
                } else if ("authorization_pending".equals(tokenResponse.getError())) {
                    log.debug("Authorization pending for device code: {}", deviceCode);
                    return PollStatusResponse.builder()
                            .status("pending")
                            .message("Waiting for user authorization")
                            .build();
                    
                } else if ("slow_down".equals(tokenResponse.getError())) {
                    log.debug("Rate limit - slow down polling");
                    return PollStatusResponse.builder()
                            .status("pending")
                            .message("Slow down polling")
                            .build();
                    
                } else if ("expired_token".equals(tokenResponse.getError())) {
                    log.warn("Device code expired: {}", deviceCode);
                    deviceCodeStore.remove(deviceCode);
                    return PollStatusResponse.builder()
                            .status("expired")
                            .message("Device code expired. Please start over.")
                            .build();
                    
                } else if ("access_denied".equals(tokenResponse.getError())) {
                    log.warn("User denied access for device code: {}", deviceCode);
                    deviceCodeStore.remove(deviceCode);
                    return PollStatusResponse.builder()
                            .status("error")
                            .message("Access denied by user")
                            .build();
                }
            }
            
        } catch (WebClientResponseException e) {
            log.error("Error polling for authorization: {} - {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error during polling", e);
        }
        
        return PollStatusResponse.builder()
                .status("pending")
                .message("Waiting for authorization")
                .build();
    }
    
    /**
     * Fetch user information from GitHub API
     */
    private GitHubUser fetchUserInfo(String accessToken) {
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
            return null;
        }
    }
    
    /**
     * Verify an access token and get user info
     */
    public GitHubUser verifyToken(String accessToken) {
        return fetchUserInfo(accessToken);
    }
}

