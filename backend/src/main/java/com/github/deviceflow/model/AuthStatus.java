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
    private boolean ghecConnected;
    private boolean ghesConnected;
    private boolean ghesEnabled;
    
    private GitHubUser azureUser;
    private GitHubUser ghecUser;
    private GitHubUser ghesUser;
    
    private String message;
}

