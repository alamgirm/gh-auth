package com.github.deviceflow.service;

import com.github.deviceflow.entity.UserToken;
import com.github.deviceflow.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLinkingService {
    
    private final UserTokenRepository userTokenRepository;
    
    /**
     * Link GitHub account to Azure user
     * Stores GitHub token with reference to Azure user ID
     * @param provider "ghec" or "ghes"
     */
    @Transactional
    public void linkGitHubAccount(String azureUserId, String githubUserId, String githubToken, String githubUsername, String provider) {
        log.info("Linking {} account {} to Azure user {}", provider.toUpperCase(), githubUsername, azureUserId);
        
        // Create a composite ID: azure:{azureId}:{provider}
        String linkedUserId = "azure:" + azureUserId + ":" + provider;
        
        Optional<UserToken> existing = userTokenRepository.findByUserId(linkedUserId);
        
        UserToken userToken;
        if (existing.isPresent()) {
            userToken = existing.get();
            userToken.setAccessToken(githubToken);
            userToken.setUsername(githubUsername);
        } else {
            userToken = UserToken.builder()
                    .userId(linkedUserId)
                    .username(githubUsername)
                    .accessToken(githubToken)
                    .tokenType("Bearer")
                    .scope("user:email read:user")
                    .build();
        }
        
        userTokenRepository.save(userToken);
        log.info("{} account linked successfully", provider.toUpperCase());
    }
    
    /**
     * Check if Azure user has GitHub provider connected
     */
    public boolean hasGitHubLinked(String azureUserId, String provider) {
        String linkedUserId = "azure:" + azureUserId + ":" + provider;
        return userTokenRepository.findByUserId(linkedUserId).isPresent();
    }
    
    /**
     * Get GitHub token for Azure user
     */
    public String getGitHubToken(String azureUserId, String provider) {
        String linkedUserId = "azure:" + azureUserId + ":" + provider;
        return userTokenRepository.findByUserId(linkedUserId)
                .map(UserToken::getAccessToken)
                .orElse(null);
    }
    
    /**
     * Get GitHub user data for Azure user
     */
    public UserToken getGitHubUserData(String azureUserId, String provider) {
        String linkedUserId = "azure:" + azureUserId + ":" + provider;
        return userTokenRepository.findByUserId(linkedUserId)
                .orElse(null);
    }
    
    /**
     * Unlink GitHub account from Azure user
     */
    @Transactional
    public void unlinkGitHubAccount(String azureUserId, String provider) {
        log.info("Unlinking {} from Azure user: {}", provider.toUpperCase(), azureUserId);
        String linkedUserId = "azure:" + azureUserId + ":" + provider;
        userTokenRepository.deleteByUserId(linkedUserId);
    }
}

