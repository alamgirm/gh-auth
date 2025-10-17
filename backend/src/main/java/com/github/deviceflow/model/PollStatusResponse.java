package com.github.deviceflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollStatusResponse {
    
    private String status; // "pending", "authorized", "expired", "error"
    
    private String accessToken;
    
    private String message;
    
    private GitHubUser user;
}

