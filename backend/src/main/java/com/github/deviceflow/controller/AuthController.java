package com.github.deviceflow.controller;

import com.github.deviceflow.model.AccessTokenResponse;
import com.github.deviceflow.model.GitHubUser;
import com.github.deviceflow.service.GitHubAuthService;
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
public class AuthController {
    
    private final GitHubAuthService authService;
    
    /**
     * Get the GitHub authorization URL for popup-based login
     * Frontend opens this URL in a popup window
     */
    @GetMapping("/authorize-url")
    public ResponseEntity<Map<String, String>> getAuthorizeUrl() {
        log.info("Received request for authorization URL");
        try {
            // Generate a random state for CSRF protection
            String state = UUID.randomUUID().toString();
            String authUrl = authService.getAuthorizationUrl(state);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", authUrl);
            response.put("state", state);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating authorization URL", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Exchange authorization code for access token
     * Frontend calls this after receiving the code from the popup callback
     */
    @PostMapping("/exchange-code")
    public ResponseEntity<Map<String, Object>> exchangeCode(
            @RequestBody Map<String, String> request) {
        String code = request.get("code");
        String state = request.get("state");
        
        log.info("Received code exchange request");
        
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // Exchange code for access token
            AccessTokenResponse tokenResponse = authService.exchangeCodeForToken(code);
            
            if (tokenResponse.getAccessToken() == null) {
                log.error("No access token in response");
                return ResponseEntity.status(401).build();
            }
            
            // Fetch user info
            GitHubUser user = authService.fetchUserInfo(tokenResponse.getAccessToken());
            
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", tokenResponse.getAccessToken());
            response.put("tokenType", tokenResponse.getTokenType());
            response.put("scope", tokenResponse.getScope());
            response.put("user", user);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            return ResponseEntity.status(401).build();
        }
    }
    
    /**
     * Verify a token and get user info
     * Frontend can use this to validate stored tokens
     */
    @GetMapping("/verify")
    public ResponseEntity<GitHubUser> verifyToken(
            @RequestHeader("Authorization") String authHeader) {
        log.info("Received token verification request");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        
        String token = authHeader.substring(7);
        
        try {
            GitHubUser user = authService.verifyToken(token);
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(401).build();
            }
        } catch (Exception e) {
            log.error("Error verifying token", e);
            return ResponseEntity.status(401).build();
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
