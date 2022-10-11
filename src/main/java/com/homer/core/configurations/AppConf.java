package com.homer.core.configurations;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
@Data
public class AppConf {
    private String clusterId;
    private String kafkaUrl;
    private String domain;
    private Integer maxThread;
    private Integer defaultPageSize;
    private Integer defaultPage;

    public String getKafkaBootstraps() {
        return this.kafkaUrl.replace(";", ",");
    }
}
