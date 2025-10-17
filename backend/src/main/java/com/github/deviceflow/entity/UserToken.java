package com.github.deviceflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_tokens")
public class UserToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String userId; // GitHub user ID
    
    @Column(nullable = false)
    private String username; // GitHub username
    
    @Column(nullable = false, length = 1000)
    private String accessToken;
    
    @Column(length = 500)
    private String tokenType;
    
    @Column(length = 500)
    private String scope;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    // User profile data (cached)
    @Column(length = 500)
    private String name;
    
    @Column(length = 500)
    private String email;
    
    @Column(length = 1000)
    private String avatarUrl;
    
    @Column(length = 2000)
    private String bio;
    
    @Column(length = 500)
    private String location;
    
    private Integer publicRepos;
    private Integer followers;
    private Integer following;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

