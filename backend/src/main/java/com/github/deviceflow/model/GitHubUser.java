package com.github.deviceflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubUser {
    
    private String login;
    
    private Long id;
    
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    private String name;
    
    private String email;
    
    private String bio;
    
    private String location;
    
    @JsonProperty("public_repos")
    private Integer publicRepos;
    
    private Integer followers;
    
    private Integer following;
    
    @JsonProperty("created_at")
    private String createdAt;
}

