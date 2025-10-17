package com.github.deviceflow.controller;

import com.github.deviceflow.model.GitHubUser;
import com.github.deviceflow.service.GitHubAuthService;
import jakarta.servlet.http.HttpSession;
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
     * Backend stores token in database and creates session
     * Frontend receives only the userId (session identifier)
     */
    @PostMapping("/exchange-code")
    public ResponseEntity<Map<String, Object>> exchangeCode(
            @RequestBody Map<String, String> request,
            HttpSession session) {
        String code = request.get("code");
        String state = request.get("state");
        
        log.info("Received code exchange request");
        
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // Exchange code for access token and store in database
            String userId = authService.exchangeCodeForToken(code);
            
            // Store userId in session
            session.setAttribute("userId", userId);
            log.info("Session created for userId: {}", userId);
            
            // Get cached user profile
            GitHubUser user = authService.getCachedUserProfile(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("user", user);
            response.put("message", "Authentication successful");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Authentication failed",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get current user info
     * Uses userId from session to fetch data from GitHub
     */
    @GetMapping("/user")
    public ResponseEntity<GitHubUser> getCurrentUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        
        if (userId == null) {
            log.warn("No userId in session");
            return ResponseEntity.status(401).build();
        }
        
        log.info("Getting user info for session userId: {}", userId);
        
        try {
            GitHubUser user = authService.getUserInfo(userId);
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(401).build();
            }
        } catch (Exception e) {
            log.error("Error getting user info", e);
            // Token might be invalid - clear session
            session.invalidate();
            return ResponseEntity.status(401).build();
        }
    }
    
    /**
     * Get cached user profile (doesn't call GitHub API)
     */
    @GetMapping("/user/cached")
    public ResponseEntity<GitHubUser> getCachedUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        
        if (userId == null) {
            log.warn("No userId in session");
            return ResponseEntity.status(401).build();
        }
        
        log.info("Getting cached user info for userId: {}", userId);
        
        GitHubUser user = authService.getCachedUserProfile(userId);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            session.invalidate();
            return ResponseEntity.status(401).build();
        }
    }
    
    /**
     * Check if user is authenticated
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAuth(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        
        Map<String, Object> response = new HashMap<>();
        
        if (userId != null && authService.hasValidToken(userId)) {
            GitHubUser user = authService.getCachedUserProfile(userId);
            response.put("authenticated", true);
            response.put("userId", userId);
            response.put("user", user);
            return ResponseEntity.ok(response);
        } else {
            response.put("authenticated", false);
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Logout - delete token from database and invalidate session
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        
        if (userId != null) {
            log.info("Logging out userId: {}", userId);
            authService.deleteUserToken(userId);
            session.invalidate();
        }
        
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
