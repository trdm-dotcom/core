package com.homer.core.configurations;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Data
public class AppConf {
    private String nodeId;
    private String clusterId;
    private String kafkaUrl;
    private String domain;
    private Integer maxThread;
    private Integer defaultPageSize;
    private Integer defaultPage;
    private Topic topics;
    private Integer timeModify;
    private Integer descriptionMaxLength;
    private Aes aes;
    private Integer timeStampHash;

    @Data
    public static class Topic {
        private String userInfo;
        private String notification;
        private String pushNotification;
        private String syncRedisMysql;
    }

    @Data
    public static class Aes {
        private String key;
        private String iv;
        private String keyHash;
    }

    public String getKafkaBootstraps() {
        return this.kafkaUrl.replace(";", ",");
    }
}
