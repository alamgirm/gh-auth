package com.github.deviceflow.controller;

import com.github.deviceflow.model.DeviceCodeResponse;
import com.github.deviceflow.model.GitHubUser;
import com.github.deviceflow.model.PollStatusResponse;
import com.github.deviceflow.service.GitHubDeviceFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final GitHubDeviceFlowService deviceFlowService;
    
    /**
     * Initiate the device flow
     * Frontend calls this to start the OAuth process
     */
    @PostMapping("/device/code")
    public ResponseEntity<DeviceCodeResponse> initiateDeviceFlow() {
        log.info("Received request to initiate device flow");
        try {
            DeviceCodeResponse response = deviceFlowService.initiateDeviceFlow();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error initiating device flow", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Poll for authorization status
     * Frontend calls this repeatedly to check if user has authorized
     */
    @GetMapping("/device/poll")
    public ResponseEntity<PollStatusResponse> pollAuthorization(
            @RequestParam("device_code") String deviceCode) {
        log.debug("Received poll request for device code: {}", deviceCode);
        try {
            PollStatusResponse response = deviceFlowService.pollForAuthorization(deviceCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error polling for authorization", e);
            return ResponseEntity.internalServerError().body(
                    PollStatusResponse.builder()
                            .status("error")
                            .message("Internal server error")
                            .build()
            );
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
            GitHubUser user = deviceFlowService.verifyToken(token);
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

