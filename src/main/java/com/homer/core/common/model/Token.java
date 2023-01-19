package com.homer.core.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Token {
    protected String domain;
    protected String userId;
    protected String serviceCode;
    protected String serviceId;
    protected String serviceName;
    protected Long clientId;
    protected Long serviceUserId;
    protected Long loginMethod;
    protected Long refreshTokenId;
    protected Long[] scopeGroupIds;
    protected String serviceUsername;
    protected UserData userData;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserData {
        protected String username;
        protected String id;
    }
}
