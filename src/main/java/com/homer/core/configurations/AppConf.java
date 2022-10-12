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
    private Topic topics;

    @Data
    private static class Topic {
        private String userInfo;
        private String notification;
        private String pushNotification;
    }

    public String getKafkaBootstraps() {
        return this.kafkaUrl.replace(";", ",");
    }
}
