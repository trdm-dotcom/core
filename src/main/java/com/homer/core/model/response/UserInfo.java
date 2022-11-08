package com.homer.core.model.response;

import com.homer.core.model.UserStatus;
import lombok.Data;

@Data
public class UserInfo {
    private String id;
    private String name;
    private Boolean isVerified;
    private UserStatus status;
    private String deviceToken;
}
