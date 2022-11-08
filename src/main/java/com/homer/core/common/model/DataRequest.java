package com.homer.core.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataRequest {
    protected Headers headers;
    protected String sourceIp;
    protected String deviceType;

    @JsonIgnore
    public String getUsername() {
        if (this.headers != null) {
            if (this.headers.getToken() != null) {
                if (this.headers.getToken().getUserData() != null) {
                    if (this.headers.getToken().getUserData().getUsername() != null) {
                        return this.getHeaders().getToken().getUserData().getUsername().toLowerCase();
                    }
                }
            }
        }
        return null;
    }

    @JsonIgnore
    public String getUsernameUpperCase() {
        if (this.headers != null) {
            if (this.headers.getToken() != null) {
                if (this.headers.getToken().getUserData() != null) {
                    if (this.headers.getToken().getUserData().getUsername() != null) {
                        return this.getHeaders().getToken().getUserData().getUsername().toUpperCase();
                    }
                }
            }
        }
        return null;
    }
}
