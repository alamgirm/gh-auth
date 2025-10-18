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
     * Get authentication status for Azure, Ghec, and Ghes
     */
    @GetMapping("/status")
    public ResponseEntity<AuthStatus> getAuthStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(AuthStatus.builder()
                    .azureAuthenticated(false)
                    .ghecConnected(false)
                    .ghesConnected(false)
                    .ghesEnabled(githubAuthService.isGhesEnabled())
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
                        .ghecConnected(false)
                        .ghesConnected(false)
                        .ghesEnabled(githubAuthService.isGhesEnabled())
                        .message("Invalid Azure token")
                        .build());
            }
            
            String azureUserId = String.valueOf(azureUser.getId());
            
            // Check Ghec connection
            boolean ghecLinked = userLinkingService.hasGitHubLinked(azureUserId, "ghec");
            GitHubUser ghecUser = null;
            if (ghecLinked) {
                String ghecToken = userLinkingService.getGitHubToken(azureUserId, "ghec");
                if (ghecToken != null) {
                    try {
                        ghecUser = githubAuthService.fetchUserInfoWithStoredToken("ghec", ghecToken);
                    } catch (Exception e) {
                        log.warn("Ghec token invalid, unlinking", e);
                        userLinkingService.unlinkGitHubAccount(azureUserId, "ghec");
                        ghecLinked = false;
                    }
                }
            }
            
            // Check Ghes connection
            boolean ghesLinked = userLinkingService.hasGitHubLinked(azureUserId, "ghes");
            GitHubUser ghesUser = null;
            if (ghesLinked) {
                String ghesToken = userLinkingService.getGitHubToken(azureUserId, "ghes");
                if (ghesToken != null) {
                    try {
                        ghesUser = githubAuthService.fetchUserInfoWithStoredToken("ghes", ghesToken);
                    } catch (Exception e) {
                        log.warn("Ghes token invalid, unlinking", e);
                        userLinkingService.unlinkGitHubAccount(azureUserId, "ghes");
                        ghesLinked = false;
                    }
                }
            }
            
            return ResponseEntity.ok(AuthStatus.builder()
                    .azureAuthenticated(true)
                    .ghecConnected(ghecLinked)
                    .ghesConnected(ghesLinked)
                    .ghesEnabled(githubAuthService.isGhesEnabled())
                    .azureUser(azureUser)
                    .ghecUser(ghecUser)
                    .ghesUser(ghesUser)
                    .message("Authenticated")
                    .build());
            
        } catch (Exception e) {
            log.error("Error getting auth status", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    // ========== GitHub Account Linking (Ghec and Ghes) ==========
    
    /**
     * Get GitHub authorization URL for linking account
     * @param provider "ghec" or "ghes"
     */
    @GetMapping("/github/{provider}/authorize-url")
    public ResponseEntity<Map<String, String>> getGitHubAuthorizeUrl(
            @PathVariable String provider,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (!"ghec".equals(provider) && !"ghes".equals(provider)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid provider"));
        }
        
        // Verify user is authenticated with Azure first
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Azure authentication required"));
        }
        
        try {
            String state = UUID.randomUUID().toString();
            String authUrl = githubAuthService.getAuthorizationUrl(provider, state);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", authUrl);
            response.put("state", state);
            response.put("provider", provider);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating {} authorization URL", provider, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Link GitHub account to Azure user
     * @param provider "ghec" or "ghes"
     */
    @PostMapping("/github/{provider}/link")
    public ResponseEntity<Map<String, Object>> linkGitHubAccount(
            @PathVariable String provider,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {
        
        if (!"ghec".equals(provider) && !"ghes".equals(provider)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid provider"));
        }
        
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
            Map<String, Object> githubData = githubAuthService.exchangeCodeWithoutStoring(provider, code);
            
            String githubToken = (String) githubData.get("token");
            GitHubUser githubUser = (GitHubUser) githubData.get("user");
            String githubUserId = (String) githubData.get("userId");
            
            log.info("{} user from exchange: {} ({})", provider.toUpperCase(), githubUser.getLogin(), githubUserId);
            log.info("Linking to Azure user: {}", azureUserId);
            
            // Link GitHub to Azure user (stores in DB as "azure:{azureId}:{provider}")
            userLinkingService.linkGitHubAccount(azureUserId, githubUserId, githubToken, githubUser.getLogin(), provider);
            
            log.info("{} account linked successfully", provider.toUpperCase());
            
            return ResponseEntity.ok(Map.of(
                    "message", provider.toUpperCase() + " account linked successfully",
                    "provider", provider,
                    "githubUser", githubUser
            ));
            
        } catch (Exception e) {
            log.error("Error linking {} account", provider, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Unlink GitHub account from Azure user
     * @param provider "ghec" or "ghes"
     */
    @PostMapping("/github/{provider}/unlink")
    public ResponseEntity<Map<String, String>> unlinkGitHubAccount(
            @PathVariable String provider,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (!"ghec".equals(provider) && !"ghes".equals(provider)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid provider"));
        }
        
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
            userLinkingService.unlinkGitHubAccount(azureUserId, provider);
            
            return ResponseEntity.ok(Map.of("message", provider.toUpperCase() + " account unlinked successfully"));
            
        } catch (Exception e) {
            log.error("Error unlinking {} account", provider, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "OK");
        status.put("ghesEnabled", githubAuthService.isGhesEnabled());
        return ResponseEntity.ok(status);
    }
}
