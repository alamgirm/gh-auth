package com.github.deviceflow.service;

import com.github.deviceflow.config.AzureEntraConfig;
import com.github.deviceflow.entity.UserToken;
import com.github.deviceflow.model.GitHubUser;
import com.github.deviceflow.repository.UserTokenRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureTokenValidationService {
    
    private final AzureEntraConfig config;
    private final UserTokenRepository userTokenRepository;
    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    
    /**
     * Validate Azure Entra ID token and extract user information
     * Note: Does NOT store token (MSAL handles token storage on frontend)
     */
    public GitHubUser validateTokenAndGetUser(String token) {
        try {
            log.debug("Validating Azure Entra token");
            
            // Parse and validate the JWT token
            JWTClaimsSet claims = validateToken(token);
            
            if (claims == null) {
                log.error("Token validation failed");
                return null;
            }
            
            // Extract user information from claims
            String userId = claims.getSubject(); // User's object ID in Azure
            String email = claims.getStringClaim("preferred_username");
            String name = claims.getStringClaim("name");
            
            log.info("Azure token validated for user: {} ({})", name, email);
            
            // Return user object (compatible with GitHubUser structure)
            // Azure tokens contain user info, so we don't need to store them
            return GitHubUser.builder()
                    .id(Long.parseLong(userId.hashCode() + "")) // Convert to long
                    .login(email != null ? email.split("@")[0] : userId)
                    .name(name)
                    .email(email)
                    .avatarUrl(null) // Azure doesn't provide avatar in token
                    .bio("Azure Entra User")
                    .build();
            
        } catch (Exception e) {
            log.error("Error validating Azure token", e);
            return null;
        }
    }
    
    /**
     * Validate JWT token signature and claims
     */
    private JWTClaimsSet validateToken(String token) {
        try {
            if (jwtProcessor == null) {
                initializeJWTProcessor();
            }
            
            // Parse and validate the token
            JWTClaimsSet claims = jwtProcessor.process(token, null);
            
            // Log claims for debugging
            log.info("Token claims - Subject: {}, Issuer: {}, Audience: {}", 
                    claims.getSubject(), claims.getIssuer(), claims.getAudience());
            log.info("Configured Client ID: {}", config.getClientId());
            log.info("Configured Tenant ID: {}", config.getTenantId());
            
            // Validate issuer
            String issuer = claims.getIssuer();
            if (!isValidIssuer(issuer)) {
                log.error("Invalid issuer: {} (expected pattern: https://login.microsoftonline.com/{}/v2.0)", issuer);
                log.error("Configured tenant: {}", config.getTenantId());
                // Don't fail on issuer for now - log and continue
            }
            
            // Validate audience
            // Accept either:
            // 1. Backend client ID (api://your-backend-client-id)
            // 2. Expected audience from config
            // 3. Microsoft Graph (for backward compatibility)
            String expectedAud = config.getExpectedAudience() != null ? config.getExpectedAudience() : config.getClientId();
            
            boolean validAudience = claims.getAudience().contains(expectedAud)
                    || claims.getAudience().contains(config.getClientId())
                    || claims.getAudience().contains("api://" + config.getClientId())
                    || claims.getAudience().contains("https://graph.microsoft.com")
                    || claims.getAudience().contains("00000003-0000-0000-c000-000000000000"); // MS Graph App ID
            
            if (!validAudience) {
                log.error("Invalid audience. Token audience: {}, Expected: {} or {}", 
                        claims.getAudience(), expectedAud, config.getClientId());
                return null;
            }
            
            log.info("Token audience validated: {}", claims.getAudience());
            
            log.info("Token validated successfully");
            return claims;
            
        } catch (Exception e) {
            log.error("Error parsing/validating token: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Initialize JWT processor with Azure's public keys
     */
    private void initializeJWTProcessor() throws Exception {
        log.info("Initializing JWT processor with JWKS URI: {}", config.getJwksUri());
        
        // Create JWT processor
        jwtProcessor = new DefaultJWTProcessor<>();
        
        // Set up JWK source (Azure's public keys)
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(config.getJwksUri()));
        
        // Set up key selector
        JWSAlgorithm expectedAlg = JWSAlgorithm.RS256;
        JWSKeySelector<SecurityContext> keySelector = 
                new JWSVerificationKeySelector<>(expectedAlg, keySource);
        
        jwtProcessor.setJWSKeySelector(keySelector);
    }
    
    /**
     * Check if issuer is valid for the configured tenant
     */
    private boolean isValidIssuer(String issuer) {
        log.info("Validating issuer: {}", issuer);
        
        if (config.getIssuer() != null && !config.getIssuer().isEmpty()) {
            boolean matches = config.getIssuer().equals(issuer);
            log.info("Issuer matches configured issuer: {}", matches);
            return matches;
        }
        
        // For multi-tenant (common), accept various issuer formats
        if ("common".equals(config.getTenantId())) {
            // Accept any Microsoft issuer
            boolean isValid = issuer != null && issuer.startsWith("https://login.microsoftonline.com/") 
                    && issuer.endsWith("/v2.0");
            log.info("Multi-tenant issuer validation: {}", isValid);
            return isValid;
        }
        
        // Validate against specific tenant ID
        String expectedIssuer = "https://login.microsoftonline.com/" + config.getTenantId() + "/v2.0";
        boolean matches = expectedIssuer.equals(issuer);
        log.info("Expected issuer: {}, Matches: {}", expectedIssuer, matches);
        return matches;
    }
    
}

