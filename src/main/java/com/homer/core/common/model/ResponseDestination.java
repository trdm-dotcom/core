package com.homer.core.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResponseDestination {
    private String topic;
    private String uri;

    public ResponseDestination(String topic, String uri) {
        this.topic = topic;
        this.uri = uri;
    }
}
