package com.homer.core.configurations;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
    private VnPayInfo vnPayInfo;
    private Integer timeModify;

    @Data
    public static class Topic {
        private String userInfo;
        private String notification;
        private String pushNotification;
        private String syncRedisMysql;
    }

    @Data
    public static class VnPayInfo {
        private String version;
        private String command;
        private String locate;
        private Integer vnPayAmountRate;
        private String datePattern;
        private String currCode;
        private String payUrl;
        private PaymentInfo paymentInfo;
        private DepositInfo depositInfo;
    }

    @Data
    public static class PaymentInfo {
        private String tmnCode;
        private String secureHash;
        private String returnUrl;
    }

    @Data
    public static class DepositInfo {
        private String tmnCode;
        private String secureHash;
        private String returnUrl;
    }

    public String getKafkaBootstraps() {
        return this.kafkaUrl.replace(";", ",");
    }
}
