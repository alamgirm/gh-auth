package com.github.deviceflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthStatus {
    
    private boolean azureAuthenticated;
    private boolean githubConnected;
    
    private GitHubUser azureUser;
    private GitHubUser githubUser;
    
    private String message;
}

