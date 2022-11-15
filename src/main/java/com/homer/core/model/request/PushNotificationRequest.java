package com.homer.core.model.request;

import com.homer.core.model.FirebaseType;
import lombok.Data;

@Data
public class PushNotificationRequest {
    private String userId;
    private String title;
    private String content;
    private String template;
    private Boolean isSave;
    private FirebaseType type;
    private String token;
    private String condition;
}
