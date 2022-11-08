package com.homer.core.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Headers {
    protected Token token;
    @JsonProperty("accept-language")
    protected String acceptLanguage;
    protected String platform;
}
