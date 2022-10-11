package com.homer.core.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Body {
    @JsonIgnore
    String getPartitionKey();
    @JsonIgnore
    String getMessageKey();
}
