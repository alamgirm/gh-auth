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
public class DeviceCodeResponse {
    
    @JsonProperty("device_code")
    private String deviceCode;
    
    @JsonProperty("user_code")
    private String userCode;
    
    @JsonProperty("verification_uri")
    private String verificationUri;
    
    @JsonProperty("expires_in")
    private Integer expiresIn;
    
    @JsonProperty("interval")
    private Integer interval;
}

