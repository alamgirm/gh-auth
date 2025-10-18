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
     */
    @Transactional
    public void linkGitHubAccount(String azureUserId, String githubUserId, String githubToken, String githubUsername) {
        log.info("Linking GitHub account {} to Azure user {}", githubUsername, azureUserId);
        
        // Create a composite ID: azure:{azureId}:github
        String linkedUserId = "azure:" + azureUserId + ":github";
        
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
        log.info("GitHub account linked successfully");
    }
    
    /**
     * Check if Azure user has GitHub connected
     */
    public boolean hasGitHubLinked(String azureUserId) {
        String linkedUserId = "azure:" + azureUserId + ":github";
        return userTokenRepository.findByUserId(linkedUserId).isPresent();
    }
    
    /**
     * Get GitHub token for Azure user
     */
    public String getGitHubToken(String azureUserId) {
        String linkedUserId = "azure:" + azureUserId + ":github";
        return userTokenRepository.findByUserId(linkedUserId)
                .map(UserToken::getAccessToken)
                .orElse(null);
    }
    
    /**
     * Unlink GitHub account from Azure user
     */
    @Transactional
    public void unlinkGitHubAccount(String azureUserId) {
        log.info("Unlinking GitHub from Azure user: {}", azureUserId);
        String linkedUserId = "azure:" + azureUserId + ":github";
        userTokenRepository.deleteByUserId(linkedUserId);
    }
}

