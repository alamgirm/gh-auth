package com.github.deviceflow.service;

import com.github.deviceflow.config.GitHubOAuthConfig;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubAuthService {
    
    private final GitHubOAuthConfig config;
    private final WebClient.Builder webClientBuilder;
    private final UserTokenRepository userTokenRepository;
    
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
     * Exchange authorization code for access token and store in database
     * Returns the userId that frontend should use for subsequent requests
     */
    public String exchangeCodeForToken(String code) {
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
                
                // Fetch user info using the new token
                GitHubUser user = fetchUserInfoWithToken(response.getAccessToken());
                
                if (user == null) {
                    throw new RuntimeException("Failed to fetch user info");
                }
                
                // Store or update token in database
                UserToken userToken = saveUserToken(user, response);
                
                log.info("Token stored for user: {}", user.getLogin());
                return userToken.getUserId();
                
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
     * Save or update user token in database
     */
    private UserToken saveUserToken(GitHubUser user, AccessTokenResponse tokenResponse) {
        String userId = String.valueOf(user.getId());
        
        Optional<UserToken> existingToken = userTokenRepository.findByUserId(userId);
        
        UserToken userToken;
        if (existingToken.isPresent()) {
            // Update existing token
            userToken = existingToken.get();
            userToken.setAccessToken(tokenResponse.getAccessToken());
            userToken.setTokenType(tokenResponse.getTokenType());
            userToken.setScope(tokenResponse.getScope());
        } else {
            // Create new token entry
            userToken = UserToken.builder()
                    .userId(userId)
                    .username(user.getLogin())
                    .accessToken(tokenResponse.getAccessToken())
                    .tokenType(tokenResponse.getTokenType())
                    .scope(tokenResponse.getScope())
                    .build();
        }
        
        // Update cached user profile data
        userToken.setName(user.getName());
        userToken.setEmail(user.getEmail());
        userToken.setAvatarUrl(user.getAvatarUrl());
        userToken.setBio(user.getBio());
        userToken.setLocation(user.getLocation());
        userToken.setPublicRepos(user.getPublicRepos());
        userToken.setFollowers(user.getFollowers());
        userToken.setFollowing(user.getFollowing());
        
        return userTokenRepository.save(userToken);
    }
    
    /**
     * Fetch user information from GitHub API using a specific token
     */
    private GitHubUser fetchUserInfoWithToken(String accessToken) {
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
     * Get user info for a given userId (fetches from GitHub using stored token)
     */
    public GitHubUser getUserInfo(String userId) {
        log.info("Getting user info for userId: {}", userId);
        
        Optional<UserToken> tokenOpt = userTokenRepository.findByUserId(userId);
        if (tokenOpt.isEmpty()) {
            log.warn("No token found for userId: {}", userId);
            return null;
        }
        
        UserToken userToken = tokenOpt.get();
        
        try {
            // Fetch fresh data from GitHub
            GitHubUser user = fetchUserInfoWithToken(userToken.getAccessToken());
            
            // Update cached profile data
            if (user != null) {
                userToken.setName(user.getName());
                userToken.setEmail(user.getEmail());
                userToken.setAvatarUrl(user.getAvatarUrl());
                userToken.setBio(user.getBio());
                userToken.setLocation(user.getLocation());
                userToken.setPublicRepos(user.getPublicRepos());
                userToken.setFollowers(user.getFollowers());
                userToken.setFollowing(user.getFollowing());
                userTokenRepository.save(userToken);
            }
            
            return user;
            
        } catch (Exception e) {
            log.error("Error fetching user info for userId: {}", userId, e);
            // Token might be invalid - delete it
            userTokenRepository.delete(userToken);
            throw new RuntimeException("Invalid or expired token");
        }
    }
    
    /**
     * Get cached user profile from database
     */
    public GitHubUser getCachedUserProfile(String userId) {
        Optional<UserToken> tokenOpt = userTokenRepository.findByUserId(userId);
        if (tokenOpt.isEmpty()) {
            return null;
        }
        
        UserToken token = tokenOpt.get();
        return GitHubUser.builder()
                .id(Long.parseLong(token.getUserId()))
                .login(token.getUsername())
                .name(token.getName())
                .email(token.getEmail())
                .avatarUrl(token.getAvatarUrl())
                .bio(token.getBio())
                .location(token.getLocation())
                .publicRepos(token.getPublicRepos())
                .followers(token.getFollowers())
                .following(token.getFollowing())
                .build();
    }
    
    /**
     * Delete user token (logout)
     */
    public void deleteUserToken(String userId) {
        log.info("Deleting token for userId: {}", userId);
        userTokenRepository.deleteByUserId(userId);
    }
    
    /**
     * Check if user has a valid token
     */
    public boolean hasValidToken(String userId) {
        return userTokenRepository.findByUserId(userId).isPresent();
    }
    
    /**
     * Get stored GitHub token for a user
     */
    public String getStoredToken(String userId) {
        return userTokenRepository.findByUserId(userId)
                .map(UserToken::getAccessToken)
                .orElse(null);
    }
    
    /**
     * Fetch user info using a provided token
     */
    public GitHubUser fetchUserInfoWithStoredToken(String token) {
        log.debug("Fetching GitHub user info with provided token");
        
        WebClient webClient = webClientBuilder.build();
        
        try {
            GitHubUser user = webClient.get()
                    .uri(config.getUserApiUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(GitHubUser.class)
                    .block();
            
            log.info("Successfully fetched GitHub user: {}", user != null ? user.getLogin() : "unknown");
            return user;
            
        } catch (Exception e) {
            log.error("Error fetching GitHub user info", e);
            throw new RuntimeException("Failed to fetch GitHub user info: " + e.getMessage());
        }
    }
    
    /**
     * Exchange code for token WITHOUT storing (for account linking)
     * Returns both token and user info
     */
    public Map<String, Object> exchangeCodeWithoutStoring(String code) {
        log.info("Exchanging GitHub code for token (no storage)");
        
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
                log.info("Successfully exchanged code for token");
                
                // Fetch user info
                GitHubUser user = fetchUserInfoWithToken(response.getAccessToken());
                
                if (user == null) {
                    throw new RuntimeException("Failed to fetch user info");
                }
                
                log.info("GitHub user: {} ({})", user.getLogin(), user.getId());
                
                // Return both without storing
                Map<String, Object> result = new HashMap<>();
                result.put("token", response.getAccessToken());
                result.put("user", user);
                result.put("userId", String.valueOf(user.getId()));
                
                return result;
                
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
}
