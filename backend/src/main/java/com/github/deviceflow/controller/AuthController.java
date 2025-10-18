package com.github.deviceflow.controller;

import com.github.deviceflow.model.AuthStatus;
import com.github.deviceflow.model.GitHubUser;
import com.github.deviceflow.service.AzureTokenValidationService;
import com.github.deviceflow.service.GitHubAuthService;
import com.github.deviceflow.service.UserLinkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {
    
    private final GitHubAuthService githubAuthService;
    private final AzureTokenValidationService azureTokenValidationService;
    private final UserLinkingService userLinkingService;
    
    // ========== Azure Primary Authentication ==========
    
    /**
     * Validate Azure token and get user info
     * Azure token is sent in Authorization header on every request
     */
    @GetMapping("/user")
    public ResponseEntity<GitHubUser> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        
        String azureToken = authHeader.substring(7);
        
        try {
            GitHubUser user = azureTokenValidationService.validateTokenAndGetUser(azureToken);
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(401).build();
            }
        } catch (Exception e) {
            log.error("Error validating Azure token", e);
            return ResponseEntity.status(401).build();
        }
    }
    
    /**
     * Get authentication status for both Azure and GitHub
     */
    @GetMapping("/status")
    public ResponseEntity<AuthStatus> getAuthStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(AuthStatus.builder()
                    .azureAuthenticated(false)
                    .githubConnected(false)
                    .message("Not authenticated")
                    .build());
        }
        
        String azureToken = authHeader.substring(7);
        
        try {
            // Validate Azure token
            GitHubUser azureUser = azureTokenValidationService.validateTokenAndGetUser(azureToken);
            
            if (azureUser == null) {
                return ResponseEntity.ok(AuthStatus.builder()
                        .azureAuthenticated(false)
                        .githubConnected(false)
                        .message("Invalid Azure token")
                        .build());
            }
            
            // Check if GitHub is linked
            String azureUserId = String.valueOf(azureUser.getId());
            boolean githubLinked = userLinkingService.hasGitHubLinked(azureUserId);
            
            GitHubUser githubUser = null;
            if (githubLinked) {
                // Get GitHub user info using stored token
                String githubToken = userLinkingService.getGitHubToken(azureUserId);
                if (githubToken != null) {
                    try {
                        githubUser = githubAuthService.fetchUserInfoWithStoredToken(githubToken);
                    } catch (Exception e) {
                        log.warn("GitHub token invalid, unlinking", e);
                        userLinkingService.unlinkGitHubAccount(azureUserId);
                        githubLinked = false;
                    }
                }
            }
            
            return ResponseEntity.ok(AuthStatus.builder()
                    .azureAuthenticated(true)
                    .githubConnected(githubLinked)
                    .azureUser(azureUser)
                    .githubUser(githubUser)
                    .message("Authenticated")
                    .build());
            
        } catch (Exception e) {
            log.error("Error getting auth status", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    // ========== GitHub Account Linking ==========
    
    /**
     * Get GitHub authorization URL for linking account
     */
    @GetMapping("/github/authorize-url")
    public ResponseEntity<Map<String, String>> getGitHubAuthorizeUrl(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Verify user is authenticated with Azure first
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Azure authentication required"));
        }
        
        try {
            String state = UUID.randomUUID().toString();
            String authUrl = githubAuthService.getAuthorizationUrl(state);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", authUrl);
            response.put("state", state);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating GitHub authorization URL", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Link GitHub account to Azure user
     */
    @PostMapping("/github/link")
    public ResponseEntity<Map<String, Object>> linkGitHubAccount(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {
        
        // Verify Azure authentication
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Azure authentication required"));
        }
        
        String azureToken = authHeader.substring(7);
        String code = request.get("code");
        
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // Validate Azure token to get user ID
            GitHubUser azureUser = azureTokenValidationService.validateTokenAndGetUser(azureToken);
            if (azureUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid Azure token"));
            }
            
            String azureUserId = String.valueOf(azureUser.getId());
            
            // Exchange GitHub code for token WITHOUT storing
            Map<String, Object> githubData = githubAuthService.exchangeCodeWithoutStoring(code);
            
            String githubToken = (String) githubData.get("token");
            GitHubUser githubUser = (GitHubUser) githubData.get("user");
            String githubUserId = (String) githubData.get("userId");
            
            log.info("GitHub user from exchange: {} ({})", githubUser.getLogin(), githubUserId);
            log.info("Linking to Azure user: {}", azureUserId);
            
            // Link GitHub to Azure user (stores in DB as "azure:{azureId}:github")
            userLinkingService.linkGitHubAccount(azureUserId, githubUserId, githubToken, githubUser.getLogin());
            
            log.info("GitHub account linked successfully");
            
            return ResponseEntity.ok(Map.of(
                    "message", "GitHub account linked successfully",
                    "githubUser", githubUser
            ));
            
        } catch (Exception e) {
            log.error("Error linking GitHub account", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Unlink GitHub account from Azure user
     */
    @PostMapping("/github/unlink")
    public ResponseEntity<Map<String, String>> unlinkGitHubAccount(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Azure authentication required"));
        }
        
        String azureToken = authHeader.substring(7);
        
        try {
            GitHubUser azureUser = azureTokenValidationService.validateTokenAndGetUser(azureToken);
            if (azureUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid Azure token"));
            }
            
            String azureUserId = String.valueOf(azureUser.getId());
            userLinkingService.unlinkGitHubAccount(azureUserId);
            
            return ResponseEntity.ok(Map.of("message", "GitHub account unlinked successfully"));
            
        } catch (Exception e) {
            log.error("Error unlinking GitHub account", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
